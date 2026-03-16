import { Routes, Route, Navigate } from 'react-router-dom'
import { AppLayout } from '@/components/layout/AppLayout'
import { PrivateRoute } from '@/components/layout/PrivateRoute'
import { LoginPage } from '@/features/auth/LoginPage'
import { DashboardPage } from '@/features/dashboard/DashboardPage'
import { RecipesPage } from '@/features/recipes/RecipesPage'
import { RecipeFormPage } from '@/features/recipes/RecipeFormPage'
import { IngredientsPage } from '@/features/ingredients/IngredientsPage'
import { IngredientFormPage } from '@/features/ingredients/IngredientFormPage'
import { CategoriesPage } from '@/features/categories/CategoriesPage'
import { UnitsPage } from '@/features/units/UnitsPage'
import { UsersPage } from '@/features/users/UsersPage'
import { UserDetailPage } from '@/features/users/UserDetailPage'
import { DietTypesPage } from '@/features/diet-types/DietTypesPage'
import { AllergiesPage } from '@/features/allergies/AllergiesPage'
import { MenusPage } from '@/features/menus/MenusPage'
import { Toaster } from '@/components/ui/Toaster'

export default function App() {
  return (
    <>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<PrivateRoute />}>
          <Route element={<AppLayout />}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/recipes" element={<RecipesPage />} />
            <Route path="/recipes/new" element={<RecipeFormPage />} />
            <Route path="/recipes/:id" element={<RecipeFormPage />} />
            <Route path="/ingredients" element={<IngredientsPage />} />
            <Route path="/ingredients/new" element={<IngredientFormPage />} />
            <Route path="/ingredients/:id" element={<IngredientFormPage />} />
            <Route path="/categories" element={<CategoriesPage />} />
            <Route path="/units" element={<UnitsPage />} />
            <Route path="/users" element={<UsersPage />} />
            <Route path="/users/:id" element={<UserDetailPage />} />
            <Route path="/diet-types" element={<DietTypesPage />} />
            <Route path="/allergies" element={<AllergiesPage />} />
            <Route path="/menus" element={<MenusPage />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
      <Toaster />
    </>
  )
}
