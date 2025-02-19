"""
    just for testing langgraph
"""
from typing import TypedDict, Annotated

from langchain_core.messages import AIMessage
from langgraph.checkpoint.memory import MemorySaver
from langgraph.graph import add_messages, StateGraph

from interface.llm.langchain_local_llm import LangChainCustomLLM


class State(TypedDict):
    messages: Annotated[list, add_messages]


graph_builder = StateGraph(State)
llm = LangChainCustomLLM(base_url="http://localhost:8001/v1", api_key="empty", model_name="CodeLlama")


def chatbot(state: State):
    ai_message = AIMessage(content=llm.invoke(state["messages"]))
    return {"messages": [ai_message]}


# The first argument is the unique node name
# The second argument is the function or object that will be called whenever
# the node is used.
memory = MemorySaver()

graph_builder.add_node("chatbot", chatbot)
graph_builder.set_entry_point("chatbot")
graph_builder.set_finish_point("chatbot")
graph = graph_builder.compile(checkpointer=memory)


def stream_graph_updates(user_input: str, thread_id: str):
    config = {"configurable": {"thread_id": thread_id}}
    events = graph.stream(
        {"messages": [("user", user_input)]}, config, stream_mode="values"
    )
    for event in events:
        event["messages"][-1].pretty_print()


while True:
    try:
        user_input = input("User: ")
        if user_input.lower() in ["quit", "exit", "q"]:
            print("Goodbye!")
            break

        stream_graph_updates(user_input, "1")
    except:
        # fallback if input() is not available
        user_input = "What do you know about LangGraph?"
        stream_graph_updates(user_input, "1")
        break
