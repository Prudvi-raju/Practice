def calculate_simple_return(start_price, end_price):
    """Calculates the percentage return of a stock."""
    return ((end_price - start_price) / start_price) * 100

print("Return: ", calculate_simple_return(100, 115), "%")
