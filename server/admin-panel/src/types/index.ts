export interface AdminUser {
  id: number
  username: string
  email: string
  is_superuser: boolean
  is_active: boolean
  first_name: string
  last_name: string
  date_joined: string
  portion_size: number
  diet_type: number | null
  diet_type_name: string | null
  allergies: number[]
}

export interface DietType {
  id: number
  name: string
}

export interface Allergy {
  id: number
  name: string
}

export interface IngredientCategory {
  id: number
  name: string
}

export interface Unit {
  id: number
  name: string
  is_base: boolean
}

export interface UnitConversion {
  id: number
  ingredient: number
  from_unit: number
  from_unit_name: string
  to_unit: number
  to_unit_name: string
  amount_per_unit: string
}

export interface IngredientNutrition {
  id: number
  base_weight: string
  base_unit: number
  protein: string
  fat: string
  carbs: string
  calories: number
}

export interface Ingredient {
  id: number
  name: string
  category: number | null
  category_name: string | null
  nutrition: IngredientNutrition | null
  unit_conversions: UnitConversion[]
  can_be_added_to_cart: boolean
  is_piece: boolean
  piece_weight: number | null
}

export interface RecipeIngredient {
  id: number
  ingredient: number
  ingredient_name: string
  amount: number
  unit: number
  unit_name: string
}

export interface RecipeStep {
  id: number
  step_number: number
  description: string
  image_url: string | null
  video_url: string | null
  timer: number | null
}

export interface Recipe {
  id: number
  title: string
  image_url: string | undefined
  cook_time: number
  servings: number
  diet_types: number[]
  diet_type_names: string[]
  meal_types: number[]
  meal_type_names: string[]
  ingredients: RecipeIngredient[]
  steps: RecipeStep[]
  total_calories: number
  total_proteins: string
  total_fats: string
  total_carbs: string
}

export interface RecipeShort {
  id: number
  title: string
  image_url: string
  cook_time: number
  servings: number
  diet_type_names: string[]
  total_calories: number
}

export interface MealType {
  id: number
  name: string
}

export interface Menu {
  id: number
  user: number
  user_email: string
  period: 'day' | 'week' | 'custom'
  start_date: string
  created_at: string
  items: MenuItem[]
}

export interface MenuItem {
  id: number
  recipe: number
  recipe_title: string
  day_offset: number
  meal_type: 'breakfast' | 'lunch' | 'dinner'
  actual_date: string
}

export interface Stats {
  recipes_count: number
  ingredients_count: number
  users_count: number
  menus_count: number
  recent_recipes: RecipeShort[]
  recent_users: AdminUser[]
}

export interface PaginatedResponse<T> {
  count: number
  next: string | null
  previous: string | null
  results: T[]
}

export interface ApiError {
  message: string
  detail?: string
}
