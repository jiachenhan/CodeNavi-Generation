package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.AssertStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoExpression;
import repair.ast.code.type.MoArrayType;
import repair.ast.declaration.MoAnonymousClassDeclaration;
import repair.ast.declaration.MoBodyDeclaration;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;


import java.io.Serial;
import java.util.Map;
import java.util.Optional;


public class MoAssertStatement extends MoStatement {
	private static final Logger logger = LoggerFactory.getLogger(MoAssertStatement.class);
	@Serial
	private static final long serialVersionUID = -4878469537115982741L;

	private final static Description<MoAssertStatement, MoExpression> expressionDescription =
			new Description<>(ChildType.CHILD, MoAssertStatement.class, MoExpression.class,
					"expression", true);

	private final static Description<MoAssertStatement, MoExpression> messageDescription =
			new Description<>(ChildType.CHILD, MoAssertStatement.class, MoExpression.class,
					"message", false);

	private final static Map<String, Description<MoAssertStatement, ?>> descriptionsMap = Map.ofEntries(
			Map.entry("expression", expressionDescription),
			Map.entry("message", messageDescription)
	);

	@RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = true)
	private MoExpression expression;

	@RoleDescriptor(type = ChildType.CHILD, role = "message", mandatory = false)
	private MoExpression message;

	public MoAssertStatement(String fileName, int startLine, int endLine, AssertStatement assertStatement) {
		super(fileName, startLine, endLine, assertStatement);
		moNodeType = MoNodeType.TYPEAssertStatement;
	}

	public void setExpression(MoExpression expression) {
		this.expression = expression;
	}

	public void setMessage(MoExpression message) {
		this.message = message;
	}
	public MoExpression getExpression() {
		return expression;
	}

	public Optional<MoExpression> getMessage() {
		return Optional.ofNullable(message);
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visitMoAssertStatement(this);
	}

	@Override
	public Object getStructuralProperty(String role) {
		Description<MoAssertStatement, ?> description = descriptionsMap.get(role);
		if(description == expressionDescription) {
			return expression;
		} else if(description == messageDescription) {
			return message;
		} else {
			logger.error("Role {} not found in MoAssertStatement", role);
			return null;
		}

	}

	@Override
	public void setStructuralProperty(String role, Object value) {
		Description<MoAssertStatement, ?> description = descriptionsMap.get(role);
		if(description == expressionDescription) {
			expression = (MoExpression) value;
		} else if(description == messageDescription) {
			message = (MoExpression) value;
		} else {
			logger.error("Role {} not found in MoAssertStatement", role);
		}
	}

	public Map<String, Description<MoAssertStatement, ?>> getDescriptionsMap() {
		return descriptionsMap;
	}

	@Override
	public Description<? extends MoNode, ?> getDescription(String role) {
		return descriptionsMap.get(role);
	}

	@Override
	public MoNode shallowClone() {
		return new MoAssertStatement(getFileName(), getStartLine(), getEndLine(), null);
	}

	@Override
	public boolean isSame(MoNode other) {
		if (other instanceof MoAssertStatement otherAssertStatement) {
			return expression.isSame(otherAssertStatement.expression) &&
					(message == null && otherAssertStatement.message == null || message != null && message.isSame(otherAssertStatement.message));
		}
		return false;
	}
}