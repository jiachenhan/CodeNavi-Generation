import string
from pathlib import Path
from typing import Optional

from app.abs.llm_4_round.inference import Analyzer as Analyzer_llm
from app.abs.llm_genpat_4_round.inference import Analyzer as Analyzer2_llm_genpat
from app.abs.classified_topdown.inference import Analyzer as Analyzer_classified
from app.abs.selected_topdown.inference import Analyzer as Analyzer_selected
from app.communication import PatternInput, _logger
from interface.java.run_java_api import java_genpat_abstract, java_llm_abstract
from interface.llm.llm_api import LLMAPI
from interface.llm.llm_openai import LLMOpenAI
from utils.config import YamlConfig

LLM_4_ROUND = "llm_4_round"
LLM_GENPAT_4_ROUND = "llm_genpat_4_round"
CLASSIFIED_TOPDOWN = "classified_topdown"
GENPAT_ONLY = "genpat_only"



def navi_abstract(_llm: LLMAPI,
                  _config: dict,
                  _pattern_input: PatternInput,
                  pattern_info_output_path: Path,
                  pattern_ori_path: Path,
                  pattern_abs_path: Path,
                  genpat_llm_json_path: Optional[Path],
                  _type: string=CLASSIFIED_TOPDOWN) -> None:
    try:
        run_analysis(_llm, _config, _pattern_input, pattern_info_output_path, pattern_ori_path, pattern_abs_path,genpat_llm_json_path, _type)
    except Exception as e:
        import traceback
        traceback.print_exc()
        _logger.error(f"Error in {pattern_info_output_path}: {e}")
        return


def run_analysis(_llm: LLMAPI,
                 _config: dict,
                 _pattern_input: PatternInput,
                 pattern_info_output_path: Path,
                 pattern_ori_path: Path,
                 pattern_abs_path: Path,
                 genpat_llm_json_path: Optional[Path],
                 _type: string
                 ):
    _jar = _config.get("jar_path")
    if _type==LLM_4_ROUND:
        analyzer = Analyzer_llm(_llm, _pattern_input, pattern_info_output_path)
        analyzer.analysis()
        java_llm_abstract(30, pattern_ori_path, pattern_info_output_path, pattern_abs_path, _jar)
    elif _type==LLM_GENPAT_4_ROUND:
        analyzer = Analyzer2_llm_genpat(_llm, _pattern_input, pattern_info_output_path, genpat_llm_json_path)
        analyzer.analysis()
        java_llm_abstract(30, pattern_ori_path, pattern_info_output_path, pattern_abs_path, _jar)
    elif _type==CLASSIFIED_TOPDOWN:
        analyzer = Analyzer_classified(_llm, _pattern_input)
        analyzer.analysis()
        analyzer.serialize(pattern_info_output_path)
        java_llm_abstract(30, pattern_ori_path, pattern_info_output_path, pattern_abs_path, _jar)
    elif _type==GENPAT_ONLY:
        java_genpat_abstract(10,pattern_ori_path,pattern_abs_path,_jar)


