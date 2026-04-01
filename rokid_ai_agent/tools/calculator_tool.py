"""计算工具 - 安全的数学表达式求值"""

import ast
import math
import operator
import logging

logger = logging.getLogger(__name__)

SAFE_OPS = {
    ast.Add: operator.add,
    ast.Sub: operator.sub,
    ast.Mult: operator.mul,
    ast.Div: operator.truediv,
    ast.FloorDiv: operator.floordiv,
    ast.Mod: operator.mod,
    ast.Pow: operator.pow,
    ast.USub: operator.neg,
}

SAFE_FUNCS = {
    "abs": abs,
    "round": round,
    "min": min,
    "max": max,
    "sum": sum,
    "sqrt": math.sqrt,
    "log": math.log,
    "log10": math.log10,
    "sin": math.sin,
    "cos": math.cos,
    "tan": math.tan,
    "pi": math.pi,
    "e": math.e,
}


def _safe_eval(node):
    if isinstance(node, ast.Expression):
        return _safe_eval(node.body)
    if isinstance(node, ast.Constant):
        if isinstance(node.value, (int, float)):
            return node.value
        raise ValueError(f"不支持的常量类型: {type(node.value)}")
    if isinstance(node, ast.BinOp):
        op_type = type(node.op)
        if op_type not in SAFE_OPS:
            raise ValueError(f"不支持的运算符: {op_type.__name__}")
        left = _safe_eval(node.left)
        right = _safe_eval(node.right)
        return SAFE_OPS[op_type](left, right)
    if isinstance(node, ast.UnaryOp):
        op_type = type(node.op)
        if op_type not in SAFE_OPS:
            raise ValueError(f"不支持的一元运算符")
        return SAFE_OPS[op_type](_safe_eval(node.operand))
    if isinstance(node, ast.Call):
        if isinstance(node.func, ast.Name) and node.func.id in SAFE_FUNCS:
            args = [_safe_eval(a) for a in node.args]
            return SAFE_FUNCS[node.func.id](*args)
        raise ValueError(f"不支持的函数")
    if isinstance(node, ast.Name) and node.id in SAFE_FUNCS:
        val = SAFE_FUNCS[node.id]
        if isinstance(val, (int, float)):
            return val
        raise ValueError("不是数值常量")
    raise ValueError(f"不支持的表达式: {ast.dump(node)}")


async def calculate(expression: str) -> str:
    """计算数学表达式

    Args:
        expression: 数学表达式，如 "2+3*4", "sqrt(144)", "sin(pi/2)"
    """
    try:
        tree = ast.parse(expression, mode="eval")
        result = _safe_eval(tree)
        formatted = f"{result:.6g}" if isinstance(result, float) else str(result)
        return f"{expression} = {formatted}"
    except ZeroDivisionError:
        return "错误: 除数不能为零"
    except Exception as e:
        return f"计算失败: {e}"


TOOL_DEFINITION = {
    "name": "calculator",
    "description": "数学计算工具，支持基本运算和数学函数（sqrt, log, sin, cos等）",
    "parameters": {
        "type": "object",
        "properties": {
            "expression": {
                "type": "string",
                "description": "数学表达式，如 '2+3*4', 'sqrt(144)', 'sin(pi/2)'",
            },
        },
        "required": ["expression"],
    },
    "handler": calculate,
}
