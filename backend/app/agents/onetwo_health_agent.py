from __future__ import annotations

from app.config import get_settings

DISCLAIMER = "Personal experiment only. Not medical advice."


def deterministic_insight(agent_input: dict) -> str:
    status = agent_input["status"]
    missing = [k for k, v in agent_input["metrics"].items() if v is None]
    missing_text = f" Missing data: {', '.join(missing)}." if missing else ""
    if status == "Elevated":
        return (
            "Several signals are elevated compared with your recent baseline. "
            "This may indicate possible physiological stress. Consider rest and monitor symptoms. "
            f"{DISCLAIMER}{missing_text}"
        )
    if status == "Watch":
        return (
            "Some signals are outside your recent baseline. This may reflect stress, poor recovery, "
            f"or early illness and is worth monitoring. {DISCLAIMER}{missing_text}"
        )
    return f"Your current signals are close to your recent baseline. Keep monitoring trends. {DISCLAIMER}{missing_text}"


def generate_health_insight(agent_input: dict) -> str:
    settings = get_settings()
    if settings.llm_provider.lower() != "onetwo":
        return deterministic_insight(agent_input)

    # TODO: LLM-generated explanations - replace the reflection-based adapter after
    # OneTwo is pinned and configured for the selected local or remote model backend.
    prompt = (
        "Write a cautious wellness insight under 120 words. Do not diagnose. "
        "Do not invent facts. Use 'may reflect' language. Mention missing data if relevant. "
        f"Structured input: {agent_input}"
    )
    try:
        # OneTwo's public API has changed across examples. Keep this isolated so the backend
        # still works with deterministic text when OneTwo is absent or differently configured.
        import onetwo  # type: ignore

        if hasattr(onetwo, "generate_text"):
            return str(onetwo.generate_text(prompt))[:1200]
        if hasattr(onetwo, "llm") and hasattr(onetwo.llm, "generate_text"):
            return str(onetwo.llm.generate_text(prompt))[:1200]
    except Exception:
        return deterministic_insight(agent_input)

    return deterministic_insight(agent_input)
