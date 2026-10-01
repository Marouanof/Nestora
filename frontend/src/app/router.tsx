import { createBrowserRouter } from "react-router-dom"

import AppLayout from "@/layout/AppLayout"
import { RequireRole } from "@/components/auth/RequireRole"
import {
  HomePage,
  LoginPage,
  RegisterPage,
  EmailVerificationPage,
  OnboardingPage,
  OnboardingOwnerPage,
  IdentityVerificationPage,
  ProfilePage,
  FAQPage,
  BlogPage,
  PropertyListPage,
  PropertyDetailsPage,
  CreatePropertyPage,
  EditPropertyPage,
  ManagePhotosPage,
  AvailabilityManagementPage,
  SubmitReviewPage,
  SearchPage,
  SearchResultsPage,
  OwnerProfilePage,
  MapPage,
  OwnerBookingsPage,
  OwnerListingsPage,
  MyBookingsPage,
  BookingDetailsPage,
  BookingSuccessPage,
  BookingCancelPage,
  WishlistPage,
  OwnerStatsPage,
  AdminDashboardPage,
  AdminPropertiesPage,
  AdminPropertyDetailPage,
  AdminReviewsPage,
  AdminUsersOverview,
  AdminBookingsPage,
  AdminUserDetails,
  AdminCreateUser,
} from "@/pages"

export const router = createBrowserRouter([
  {
    path: "/",
    element: <AppLayout />,
    children: [
      // Public routes
      { index: true, element: <HomePage /> },
      { path: "login", element: <LoginPage /> },
      { path: "register", element: <RegisterPage /> },
      { path: "verify-email", element: <EmailVerificationPage /> },
      {
        path: "onboarding",
        element: (
          <RequireRole roles={["ROLE_TENANT"]}>
            <OnboardingPage />
          </RequireRole>
        ),
      },
      {
        path: "owner/onboarding",
        element: (
          <RequireRole roles={["ROLE_OWNER"]}>
            <OnboardingOwnerPage />
          </RequireRole>
        ),
      },
      {
        path: "owner/verification",
        element: (
          <RequireRole roles={["ROLE_OWNER"]}>
            <IdentityVerificationPage />
          </RequireRole>
        ),
      },
      { path: "properties", element: <PropertyListPage /> },
      { path: "properties/:id", element: <PropertyDetailsPage /> },
      { path: "faq", element: <FAQPage /> },
      { path: "blog", element: <BlogPage /> },
      { path: "search", element: <SearchPage /> },
      { path: "search/results", element: <SearchResultsPage /> },
      { path: "users/:id", element: <OwnerProfilePage /> },
      { path: "map", element: <MapPage /> },
      { path: "how-it-works", element: <div>How It Works Page</div> },

      // Authenticated user routes (any role)
      {
        path: "profile",
        element: (
          <RequireRole roles={["ROLE_TENANT", "ROLE_OWNER", "ROLE_ADMIN"]}>
            <ProfilePage />
          </RequireRole>
        ),
      },
      {
        path: "wishlist",
        element: (
          <RequireRole roles={["ROLE_TENANT", "ROLE_OWNER", "ROLE_ADMIN"]}>
            <WishlistPage />
          </RequireRole>
        ),
      },

      // Tenant routes
      {
        path: "tenant/bookings",
        element: (
          <RequireRole roles={["ROLE_TENANT"]}>
            <MyBookingsPage />
          </RequireRole>
        ),
      },
      {
        path: "tenant/bookings/:id",
        element: (
          <RequireRole roles={["ROLE_TENANT"]}>
            <BookingDetailsPage />
          </RequireRole>
        ),
      },

      // Owner routes
      {
        path: "owner/properties",
        element: (
          <RequireRole roles={["ROLE_OWNER"]}>
            <OwnerListingsPage />
          </RequireRole>
        ),
      },
      {
        path: "owner/properties/new",
        element: (
          <RequireRole roles={["ROLE_OWNER"]}>
            <CreatePropertyPage />
          </RequireRole>
        ),
      },
      {
        path: "owner/bookings",
        element: (
          <RequireRole roles={["ROLE_OWNER"]}>
            <OwnerBookingsPage />
          </RequireRole>
        ),
      },
      {
        path: "owner/stats",
        element: (
          <RequireRole roles={["ROLE_OWNER"]}>
            <OwnerStatsPage />
          </RequireRole>
        ),
      },

      // Admin routes
      {
        path: "admin",
        element: (
          <RequireRole roles={["ROLE_ADMIN"]}>
            <AdminDashboardPage />
          </RequireRole>
        ),
      },
      {
        path: "admin/properties",
        element: (
          <RequireRole roles={["ROLE_ADMIN"]}>
            <AdminPropertiesPage />
          </RequireRole>
        ),
      },
      {
        path: "admin/properties/:id",
        element: (
          <RequireRole roles={["ROLE_ADMIN"]}>
            <AdminPropertyDetailPage />
          </RequireRole>
        ),
      },
      {
        path: "admin/reviews",
        element: (
          <RequireRole roles={["ROLE_ADMIN"]}>
            <AdminReviewsPage />
          </RequireRole>
        ),
      },
      {
        path: "admin/users",
        element: (
          <RequireRole roles={["ROLE_ADMIN"]}>
            <AdminUsersOverview />
          </RequireRole>
        ),
      },
      {
        path: "admin/users/create",
        element: (
          <RequireRole roles={["ROLE_ADMIN"]}>
            <AdminCreateUser />
          </RequireRole>
        ),
      },
      {
        path: "admin/users/:id",
        element: (
          <RequireRole roles={["ROLE_ADMIN"]}>
            <AdminUserDetails />
          </RequireRole>
        ),
      },
      {
        path: "admin/bookings",
        element: (
          <RequireRole roles={["ROLE_ADMIN"]}>
            <AdminBookingsPage />
          </RequireRole>
        ),
      },

      // Legacy routes (for backward compatibility)
      {
        path: "my-listings",
        element: (
          <RequireRole roles={["ROLE_OWNER"]}>
            <OwnerListingsPage />
          </RequireRole>
        ),
      },
      {
        path: "create-property",
        element: (
          <RequireRole roles={["ROLE_OWNER"]}>
            <CreatePropertyPage />
          </RequireRole>
        ),
      },
      { path: "properties/:id/edit", element: <EditPropertyPage /> },
      { path: "properties/:id/photos", element: <ManagePhotosPage /> },
      { path: "properties/:id/availability", element: <AvailabilityManagementPage /> },
      { path: "properties/:id/review", element: <SubmitReviewPage /> },
      {
        path: "bookings",
        element: (
          <RequireRole roles={["ROLE_TENANT"]}>
            <MyBookingsPage />
          </RequireRole>
        ),
      },
      {
        path: "bookings/success",
        element: (
          <RequireRole roles={["ROLE_TENANT"]}>
            <BookingSuccessPage />
          </RequireRole>
        ),
      },
      {
        path: "bookings/cancel",
        element: (
          <RequireRole roles={["ROLE_TENANT"]}>
            <BookingCancelPage />
          </RequireRole>
        ),
      },
      {
        path: "bookings/:id",
        element: (
          <RequireRole roles={["ROLE_TENANT"]}>
            <BookingDetailsPage />
          </RequireRole>
        ),
      },
      { path: "my-rentals", element: <div>My Rentals Page</div> },
    ],
  },
])

export default router