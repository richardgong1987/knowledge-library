"""Lambda handler: count today's Lemonade orders and add up the money."""


def handler(event, context):
    orders = event.get("orders", [])
    return {
        "orders": len(orders),
        "revenue": round(sum(order["price"] * order["cups"] for order in orders), 2),
    }
