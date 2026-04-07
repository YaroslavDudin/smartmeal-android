def convert_amount(ingredient, amount, unit, target_unit):
    if target_unit.pk == unit.pk:
        return amount

    conversions = ingredient.unit_conversions.all()
    
    for conv in conversions:
        # Прямая: self.unit → target_unit
        if conv.from_unit_id == unit.pk and conv.to_unit_id == target_unit.pk:
            return amount * conv.amount_per_unit
        # Обратная: target_unit → self.unit
        if conv.from_unit_id == target_unit.pk and conv.to_unit_id == unit.pk:
            return amount / conv.amount_per_unit
    
    return None
