// static/script.js
// 新版：左侧数据集多级导航 + 方法列表 + 右侧代码视图

// 全局状态
let allItems = [];
let currentFilter = 'all';      // all | tp | fp | fn
let currentSearch = '';         // 搜索关键字
let currentItemIndex = -1;       // 当前选中的方法在 allItems 中的下标
let currentFilteredIndex = -1;

// 多级导航当前选择
let currentDataset = null;
let currentChecker = null;
let currentGroup = null;
let currentCaseInfo = null;
let currentTreeKey = null;      // 用于高亮树节点的唯一 key

// 入口：加载数据
function loadResults() {
    fetch('data.json')
        .then(r => r.json())
        .then(data => {
            allItems = data || [];

            // 默认不限定 dataset/checker/group/case，展示所有方法
            renderNavTree();
            renderMethodList();

            if (allItems.length > 0) {
                console.log(`Loaded ${allItems.length} items`);
                currentItemIndex = 0;
                renderCode(allItems[0]);
                markActiveMethod();
            }
        })
        .catch(err => {
            console.error('Failed to load data.json', err);
            const sidebar = document.querySelector('.nav-tree');
            if (sidebar) {
                sidebar.innerHTML = '<div class="no-results">无法加载数据文件 data.json</div>';
            }
        });
}

// 计算过滤后的方法列表（受 label / 搜索 / 多级导航影响）
function getFilteredItems() {
    const keyword = (currentSearch || '').trim().toLowerCase();
    return allItems.filter(item => {
        // label 过滤
        if (currentFilter !== 'all' && item.label !== currentFilter) return false;

        // dataset 层级过滤
        if (currentDataset && item.dataset !== currentDataset) return false;
        if (currentChecker && item.checker !== currentChecker) return false;
        if (currentGroup && item.group !== currentGroup) return false;
        if (currentCaseInfo && item.case_info !== currentCaseInfo) return false;

        // 搜索过滤：方法签名或文件名
        if (keyword) {
            const sig = (item.method_signature || '').toLowerCase();
            const file = (item.file_name || '').toLowerCase();
            if (!sig.includes(keyword) && !file.includes(keyword)) {
                return false;
            }
        }
        return true;
    });
}

// 新增：根据当前选中项找到在新筛选结果中的正确位置
function updateCurrentItemIndex() {
    if (currentItemIndex < 0 || !allItems[currentItemIndex]) {
        console.log('Invalid currentItemIndex, resetting to 0');
        currentItemIndex = 0;
        currentFilteredIndex = 0;
        return;
    }

    const currentItem = allItems[currentItemIndex];
    const filtered = getFilteredItems();

    console.log(`Looking for item at allItems[${currentItemIndex}]:`, currentItem.method_signature);
    console.log(`Filtered items count: ${filtered.length}`);

    // 在筛选结果中寻找当前项的索引
    // 在筛选结果中寻找当前项的索引 - 使用不同的变量名
    const foundFilteredIndex = filtered.findIndex(item =>
        item.dataset === currentItem.dataset &&
        item.checker === currentItem.checker &&
        item.group === currentItem.group &&
        item.case_info === currentItem.case_info &&
        item.method_signature === currentItem.method_signature &&
        item.file_name === currentItem.file_name &&
        item.begin_line === currentItem.begin_line
    );

    if (foundFilteredIndex >= 0) {
        console.log(`Found at filtered index: ${foundFilteredIndex}`);
        currentItemIndex = foundFilteredIndex;
    } else {
        console.log('Item not found in filtered results, selecting first available');
        // 如果找不到（被过滤掉了），选择第一个
        currentItemIndex = filtered.length > 0 ? 0 : -1;
    }
}

// 构建并渲染左侧多级导航树（dataset / checker / group / case）
function renderNavTree() {
    const treeContainer = document.querySelector('.nav-tree');
    if (!treeContainer) return;

    // 按层级聚合
    const tree = {};
    allItems.forEach(item => {
        const ds = item.dataset || 'unknown_dataset';
        const checker = item.checker || 'unknown_checker';
        const group = item.group || 'unknown_group';
        const caseInfo = item.case_info || 'unknown_case';

        if (!tree[ds]) tree[ds] = {};
        if (!tree[ds][checker]) tree[ds][checker] = {};
        if (!tree[ds][checker][group]) tree[ds][checker][group] = new Set();
        tree[ds][checker][group].add(caseInfo);
    });

    treeContainer.innerHTML = '';

    const createNode = (label, level, key, onClick) => {
        const node = document.createElement('div');
        node.className = `tree-node tree-level-${level}`;
        node.dataset.key = key;
        node.innerHTML = `
            <div class="tree-label">
                <span class="tree-arrow"></span>
                <span class="tree-text">${label}</span>
            </div>
            <div class="tree-children"></div>
        `;
        // 初始为折叠状态（所有层级关闭）
        node.classList.add('collapsed');
        const labelEl = node.querySelector('.tree-label');
        labelEl.addEventListener('click', () => {
            // 折叠/展开
            node.classList.toggle('collapsed');
            if (typeof onClick === 'function') {
                onClick();
            }
            markActiveTreeNode();
            renderMethodList();
        });
        return node;
    };

    Object.keys(tree).sort().forEach(ds => {
        const dsKey = `${ds}`;
        const dsNode = createNode(ds, 1, dsKey, () => {
            setCurrentScope(ds, null, null, null, dsKey);
        });
        const dsChildren = dsNode.querySelector('.tree-children');

        Object.keys(tree[ds]).sort().forEach(checker => {
            const checkerKey = `${ds}|${checker}`;
            const checkerNode = createNode(checker, 2, checkerKey, () => {
                setCurrentScope(ds, checker, null, null, checkerKey);
            });
            const checkerChildren = checkerNode.querySelector('.tree-children');

            Object.keys(tree[ds][checker]).sort().forEach(group => {
                const groupKey = `${ds}|${checker}|${group}`;
                const groupNode = createNode(group, 3, groupKey, () => {
                    setCurrentScope(ds, checker, group, null, groupKey);
                });
                const groupChildren = groupNode.querySelector('.tree-children');

                Array.from(tree[ds][checker][group]).sort().forEach(caseInfo => {
                    const caseKey = `${ds}|${checker}|${group}|${caseInfo}`;
                    const caseNode = document.createElement('div');
                    caseNode.className = 'tree-node tree-level-4 tree-leaf';
                    caseNode.dataset.key = caseKey;
                    caseNode.innerHTML = `<div class="tree-label"><span class="tree-dot"></span><span class="tree-text">${caseInfo}</span></div>`;
                    const labelEl = caseNode.querySelector('.tree-label');
                    labelEl.addEventListener('click', () => {
                        setCurrentScope(ds, checker, group, caseInfo, caseKey);
                        markActiveTreeNode();
                        renderMethodList();
                    });
                    groupChildren.appendChild(caseNode);
                });

                checkerChildren.appendChild(groupNode);
            });

            dsChildren.appendChild(checkerNode);
        });

        treeContainer.appendChild(dsNode);
    });

    // 如果已有选中的节点，展开路径
    if (currentTreeKey) {
        expandPathForKey(currentTreeKey);
    }
    markActiveTreeNode();
}

// 更新当前多级导航 scope
function setCurrentScope(ds, checker, group, caseInfo, key) {
    currentDataset = ds;
    currentChecker = checker;
    currentGroup = group;
    currentCaseInfo = caseInfo;
    currentTreeKey = key;
}

// 高亮当前树节点
function markActiveTreeNode() {
    const nodes = document.querySelectorAll('.tree-node');
    nodes.forEach(node => {
        if (node.dataset.key === currentTreeKey) {
            node.classList.add('active');
        } else {
            node.classList.remove('active');
        }
    });
}

// 根据 key 展开树路径
function expandPathForKey(key) {
    if (!key) return;
    const parts = key.split('|');
    const keysToOpen = [];
    for (let i = 0; i < parts.length; i++) {
        keysToOpen.push(parts.slice(0, i + 1).join('|'));
    }
    keysToOpen.forEach(k => {
        const node = document.querySelector(`.tree-node[data-key="${k}"]`);
        if (node) {
            node.classList.remove('collapsed');
        }
    });
    const leaf = document.querySelector(`.tree-node[data-key="${key}"]`);
    if (leaf && leaf.scrollIntoView) {
        leaf.scrollIntoView({ block: 'nearest' });
    }
}

// 新增：关闭指定父级下的所有子节点（除了指定的key）
function collapseSiblings(parentKey, keepExpandedKey) {
    if (!parentKey) return;

    const parentSelectors = parentKey.split('|').length;
    let selector = '';

    // 根据层级构建选择器
    for (let i = 0; i < parentSelectors; i++) {
        if (i > 0) selector += ', ';
        selector += `.tree-node[data-key^="${parentKey.split('|').slice(0, i + 1).join('|')}"]`;
    }

    const parentNodes = document.querySelectorAll(selector);
    parentNodes.forEach(node => {
        if (node.dataset.key !== keepExpandedKey) {
            node.classList.add('collapsed');
        }
    });
}

// 新增：关闭所有顶级节点（除了指定的）
function collapseAllTopLevel(exceptKey = null) {
    const topLevelNodes = document.querySelectorAll('.tree-node.tree-level-1');
    topLevelNodes.forEach(node => {
        if (node.dataset.key !== exceptKey) {
            node.classList.add('collapsed');
        }
    });
}


// 渲染左侧方法列表（右侧列）
function renderMethodList() {
    const container = document.querySelector('.nav-method-list');
    if (!container) return;

    const filtered = getFilteredItems();

    // 关键：在渲染前更新currentItemIndex
    updateCurrentItemIndex();

    container.innerHTML = '';

    if (filtered.length === 0) {
        container.innerHTML = '<div class="no-results">未找到匹配的方法</div>';
        return;
    }

    filtered.forEach((item, index) => {
        const el = document.createElement('div');
        el.className = `nav-method label-${item.label}`;
        el.dataset.label = item.label;
        el.dataset.method = item.method_signature || '';
        el.dataset.index = index; // ← 添加index到dataset便于调试

        // 添加data属性存储原始数据引用，便于精确匹配
        el.dataset.dataset = item.dataset;
        el.dataset.checker = item.checker;
        el.dataset.group = item.group;
        el.dataset.caseInfo = item.case_info;
        el.dataset.signature = item.method_signature;

        el.innerHTML = `
            <div class="method-title">${item.method_signature}</div>
            <div class="method-meta">
                <span class="method-file">${item.file_name}</span>
                <span class="method-lines">Lines ${item.begin_line}-${item.end_line}</span>
            </div>
            <span class="method-badge badge-${item.label}">${(item.label || '').toUpperCase()}</span>
        `;
        el.addEventListener('click', () => {
            const clickedItem = filtered[index]; // 直接从当前filtered数组获取

            // 在allItems中找到这个项目的确切位置
            currentItemIndex = allItems.findIndex(originalItem =>
                originalItem.dataset === clickedItem.dataset &&
                originalItem.checker === clickedItem.checker &&
                originalItem.group === clickedItem.group &&
                originalItem.case_info === clickedItem.case_info &&
                originalItem.method_signature === clickedItem.method_signature &&
                originalItem.file_name === clickedItem.file_name && // 增加更多匹配条件
                originalItem.begin_line === clickedItem.begin_line
            );

            if (currentItemIndex < 0) {
                console.warn('Could not find item in allItems:', clickedItem);
                currentItemIndex = 0;
            }

            // 立即更新filtered索引以保持同步
            currentFilteredIndex = index; // ← 直接使用当前的index

            // 点击方法时，树导航同步到对应的 dataset / checker / group / case
            const ds = item.dataset || 'unknown_dataset';
            const checker = item.checker || 'unknown_checker';
            const group = item.group || 'unknown_group';
            const caseInfo = item.case_info || 'unknown_case';
            const key = `${ds}|${checker}|${group}|${caseInfo}`;

            setCurrentScope(item.dataset, item.checker, item.group, item.case_info, key);
            expandPathForKey(key);
            markActiveTreeNode();

            // renderMethodList();
            renderCode(item);
            markActiveMethod();
        });
        container.appendChild(el);
    });

    markActiveMethod();
}

// 高亮当前选中的方法，并在列表中高亮搜索关键字
function markActiveMethod() {
    const container = document.querySelector('.nav-method-list');
    if (!container) return;

    const keyword = (currentSearch || '').trim().toLowerCase();
    const filtered = getFilteredItems();

    const methodNodes = container.querySelectorAll('.nav-method');

    const targetIndex = currentFilteredIndex >= 0 ? currentFilteredIndex : 0;

    methodNodes.forEach((node, idx) => {
        node.classList.remove('active');

        if (idx === targetIndex) {
            console.log(`Highlighting node at filtered index ${idx}`);
            node.classList.add('active');
        }

        if (keyword) {
            const titleEl = node.querySelector('.method-title');
            const fileEl = node.querySelector('.method-file');
            if (titleEl) {
                titleEl.innerHTML = highlightPlainText(titleEl.textContent || '', keyword);
            }
            if (fileEl) {
                fileEl.innerHTML = highlightPlainText(fileEl.textContent || '', keyword);
            }
        }
    });
}

// 渲染右侧代码区域
function renderCode(item) {
    const content = document.querySelector('.content');
    if (!content) return;

    const keyword = (currentSearch || '').trim();
    const safeSource = escapeHtml(item.method_source || '');
    const highlightedSource = keyword
        ? highlightHtml(safeSource, keyword)
        : safeSource;

    const naviLine = item.navi_warning && item.navi_warning.report_line
        ? item.navi_warning.report_line
        : 'N/A';

    const dslRaw = item.dsl_source || '';
    const safeDslSource = escapeHtml(dslRaw);
    const highlightedDslSource = keyword
        ? highlightHtml(safeDslSource, keyword)
        : safeDslSource;

    const buggyRaw = item.buggy_code || '';
    const safeBuggyCode = escapeHtml(buggyRaw);
    const highlightedBuggyCode = keyword
        ? highlightHtml(safeBuggyCode, keyword)
        : safeBuggyCode;

    const fixedRaw = item.fixed_code || '';
    const safeFixedCode = escapeHtml(fixedRaw);
    const highlightedFixedCode = keyword
        ? highlightHtml(safeFixedCode, keyword)
        : safeFixedCode;

    content.innerHTML = `
        <div class="code-panel code-panel-main">
            <div class="code-header">
                <div class="code-header-top">
                    <div class="code-title">${item.method_signature || ''}</div>
                    <div class="code-label">
                        <span class="pill-label badge-${item.label}">${(item.label || '').toUpperCase()}</span>
                    </div>
                </div>
                <div class="code-header-meta">
                    <div>
                        <span class="meta-label">📦 Dataset / Checker / Group</span>
                        <span class="meta-value">
                            ${item.dataset || ''}
                            ${item.checker ? ' / ' + item.checker : ''}
                            ${item.group ? ' / ' + item.group : ''}
                        </span>
                    </div>
                    <div>
                        <span class="meta-label">📁 File</span>
                        <span class="meta-value">${item.file_name || ''}</span>
                    </div>
                    <div>
                        <span class="meta-label">📍 Lines</span>
                        <span class="meta-value">${item.begin_line} - ${item.end_line}</span>
                    </div>
                    <div>
                        <span class="meta-label">🔍 Navi Line</span>
                        <span class="meta-value">${naviLine}</span>
                    </div>
                </div>
            </div>
            <pre class="code-block"><code>${highlightedSource}</code></pre>
        </div>

        <div class="code-panel code-panel-dsl">
            <div class="code-header code-header-secondary">
                <div class="code-header-top">
                    <div class="code-title">DSL Case: ${item.case_info || ''}</div>
                    <div class="code-label">
                        <span class="pill-label pill-label-dsl">DSL</span>
                    </div>
                </div>
                ${
                    item.may_be_fixed_violations
                        ? `<div class="code-header-meta">
                            <div>
                                <span class="meta-label">ℹ️ May Be Fixed Violations</span>
                                <span class="meta-value">${escapeHtml(item.may_be_fixed_violations)}</span>
                            </div>
                        </div>`
                        : ''
                }
            </div>
            <div class="dsl-content-wrapper">
                <div class="dsl-code-dsl">
                    <div class="code-pair-item dsl-only-item">
                        ${
                            dslRaw
                                ? `<pre class="code-block code-block-dsl"><code>${highlightedDslSource}</code></pre>`
                                : `<div class="dsl-empty">未找到对应的 DSL (.kirin) 文件</div>`
                        }
                    </div>
                </div>
                <div class="dsl-code-main">
                    <div class="code-pair-item">
                        <div class="code-pair-header">Buggy Code</div>
                        ${
                            item.buggy_code
                                ? `<pre class="code-block code-block-pair"><code>${highlightedBuggyCode}</code></pre>`
                                : `<div class="code-empty">未找到 buggy.java</div>`
                        }
                    </div>
                    <div class="code-pair-item">
                        <div class="code-pair-header">Fixed Code</div>
                        ${
                            item.fixed_code
                                ? `<pre class="code-block code-block-pair"><code>${highlightedFixedCode}</code></pre>`
                                : `<div class="code-empty">未找到 fixed.java</div>`
                        }
                    </div>
                </div>
            </div>
        </div>
    `;

    content.scrollTop = 0;
}

// 文本转义为安全 HTML
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// 在纯文本中高亮关键字（用于列表）
function highlightPlainText(text, keyword) {
    if (!keyword) return escapeHtml(text);
    const safeKeyword = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const reg = new RegExp(safeKeyword, 'gi');
    const escaped = escapeHtml(text);
    return escaped.replace(reg, match => `<mark class="text-highlight">${match}</mark>`);
}

// 在已经转义过的 HTML 源码中高亮关键字（用于代码区）
function highlightHtml(escapedHtml, keyword) {
    if (!keyword) return escapedHtml;
    const safeKeyword = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const reg = new RegExp(safeKeyword, 'gi');
    return escapedHtml.replace(reg, match => `<mark class="code-highlight">${match}</mark>`);
}

// 由搜索框调用
function filterMethods(query = '') {
    currentSearch = query || '';
    renderMethodList();
}

// 由顶部 TP/FP/FN 按钮调用
function filterByLabel(label) {
    currentFilter = label;

    // 切换按钮状态
    document.querySelectorAll('.filter-btn').forEach(btn => btn.classList.remove('active'));
    if (typeof event !== 'undefined' && event.target) {
        event.target.classList.add('active');
    }

    renderMethodList();
}

document.addEventListener('DOMContentLoaded', () => {
    loadResults();
});
