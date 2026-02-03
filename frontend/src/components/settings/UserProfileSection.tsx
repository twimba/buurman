import { useState } from 'react';
import { User, Mail, Lock, Camera, Save, X, Loader2 } from 'lucide-react';
import {
  useCurrentUser,
  useUpdateUserProfile,
} from '../../hooks/useUserPreferencesHooks';

export const UserProfileSection = () => {
  const { data: currentUser, isLoading } = useCurrentUser();
  const updateProfileMutation = useUpdateUserProfile();

  const [isEditing, setIsEditing] = useState(false);
  const [userData, setUserData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    avatarUrl: null as string | null,
  });

  const [lastSyncedUser, setLastSyncedUser] = useState(currentUser);
  if (currentUser && currentUser !== lastSyncedUser) {
    setLastSyncedUser(currentUser);
    setUserData({
      firstName: currentUser.firstName || '',
      lastName: currentUser.lastName || '',
      email: currentUser.email || '',
      avatarUrl: null,
    });
  }

  const handleStartEdit = () => {
    setIsEditing(true);
  };

  const handleCancelEdit = () => {
    if (currentUser) {
      setUserData({
        firstName: currentUser.firstName || '',
        lastName: currentUser.lastName || '',
        email: currentUser.email || '',
        avatarUrl: null,
      });
    }
    setIsEditing(false);
  };

  const handleSaveProfile = () => {
    updateProfileMutation.mutate(
      {
        firstName: userData.firstName,
        lastName: userData.lastName,
      },
      {
        onSuccess: () => setIsEditing(false),
      }
    );
  };

  const handleAvatarUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const previewUrl = URL.createObjectURL(file);
      setUserData({ ...userData, avatarUrl: previewUrl });
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Profile Information Card */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-900">
        <div className="p-6 border-b border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
                Profile Information
              </h2>
              <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                Update your personal information and avatar
              </p>
            </div>
            {!isEditing && (
              <button
                onClick={handleStartEdit}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 dark:hover:bg-blue-600 transition-colors"
              >
                Edit Profile
              </button>
            )}
          </div>
        </div>

        <div className="p-6">
          {/* Avatar Section */}
          <div className="flex items-center gap-6 mb-6 pb-6 border-b border-gray-200 dark:border-gray-700">
            <div className="relative">
              {userData.avatarUrl ? (
                <img
                  src={userData.avatarUrl}
                  alt="Profile"
                  className="h-24 w-24 rounded-full object-cover"
                />
              ) : (
                <div className="h-24 w-24 rounded-full bg-blue-100 dark:bg-blue-900 flex items-center justify-center">
                  <User className="h-12 w-12 text-blue-600 dark:text-blue-400" />
                </div>
              )}
              {isEditing && (
                <label
                  htmlFor="avatar-upload"
                  className="absolute bottom-0 right-0 h-8 w-8 bg-blue-600 dark:bg-blue-700 rounded-full flex items-center justify-center cursor-pointer hover:bg-blue-700 dark:hover:bg-blue-600 transition-colors"
                >
                  <Camera className="h-4 w-4 text-white" />
                  <input
                    id="avatar-upload"
                    type="file"
                    accept="image/*"
                    onChange={handleAvatarUpload}
                    className="hidden"
                  />
                </label>
              )}
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100">
                {userData.firstName} {userData.lastName}
              </h3>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                {userData.email}
              </p>
            </div>
          </div>

          {/* Profile Form */}
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  First Name
                </label>
                <input
                  type="text"
                  value={userData.firstName}
                  onChange={(e) =>
                    setUserData({ ...userData, firstName: e.target.value })
                  }
                  disabled={!isEditing}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100 dark:disabled:bg-gray-600 disabled:cursor-not-allowed"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  Last Name
                </label>
                <input
                  type="text"
                  value={userData.lastName}
                  onChange={(e) =>
                    setUserData({ ...userData, lastName: e.target.value })
                  }
                  disabled={!isEditing}
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 dark:bg-gray-700 dark:text-gray-100 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-gray-100 dark:disabled:bg-gray-600 disabled:cursor-not-allowed"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                Email Address
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400 dark:text-gray-600" />
                <input
                  type="email"
                  value={userData.email}
                  disabled
                  className="w-full pl-10 pr-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-gray-100 dark:bg-gray-700 dark:text-gray-400 cursor-not-allowed"
                />
              </div>
              <p className="text-xs text-gray-500 dark:text-gray-500 mt-1">
                Email address cannot be changed. Contact support if you need to
                update it.
              </p>
            </div>

            {isEditing && (
              <div className="flex justify-end gap-3 pt-4 border-t border-gray-200 dark:border-gray-700">
                <button
                  onClick={handleCancelEdit}
                  className="px-4 py-2 border border-gray-300 dark:border-gray-600 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors flex items-center gap-2"
                >
                  <X className="h-4 w-4" />
                  Cancel
                </button>
                <button
                  onClick={handleSaveProfile}
                  disabled={updateProfileMutation.isPending}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-50"
                >
                  {updateProfileMutation.isPending ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Save className="h-4 w-4" />
                  )}
                  Save Changes
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Security Settings Card */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow dark:shadow-gray-900">
        <div className="p-6 border-b border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-gray-900 dark:text-gray-100">
                Security Settings
              </h2>
              <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                Manage your password and security preferences
              </p>
            </div>
          </div>
        </div>

        <div className="p-6">
          <div className="flex items-center justify-between p-4 bg-gray-50 dark:bg-gray-700 rounded-lg">
            <div className="flex items-center gap-3">
              <Lock className="h-5 w-5 text-gray-600 dark:text-gray-400" />
              <div>
                <p className="font-medium text-gray-900 dark:text-gray-100">
                  Password
                </p>
                <p className="text-sm text-gray-600 dark:text-gray-400">
                  Password changes are managed through your identity provider
                </p>
              </div>
            </div>
            <a
              href="/auth/change-password"
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              Change Password
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};
