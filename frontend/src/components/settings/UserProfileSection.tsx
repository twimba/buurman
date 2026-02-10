import { useState } from 'react';
import { User, Mail, Phone, Lock, Camera, Save, X, Loader2 } from 'lucide-react';
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
    phone: '',
    avatarUrl: null as string | null,
  });

  const [lastSyncedUser, setLastSyncedUser] = useState(currentUser);
  if (currentUser && currentUser !== lastSyncedUser) {
    setLastSyncedUser(currentUser);
    setUserData({
      firstName: currentUser.firstName || '',
      lastName: currentUser.lastName || '',
      email: currentUser.email || '',
      phone: currentUser.phone || '',
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
        phone: currentUser.phone || '',
        avatarUrl: null,
      });
    }
    setIsEditing(false);
  };

  const [phoneError, setPhoneError] = useState('');

  const handleSaveProfile = () => {
    if (userData.phone && !/^\+[1-9]\d{1,14}$/.test(userData.phone)) {
      setPhoneError('Phone must be in E.164 format (e.g., +31612345678)');
      return;
    }
    setPhoneError('');
    updateProfileMutation.mutate(
      {
        firstName: userData.firstName,
        lastName: userData.lastName,
        phone: userData.phone || null,
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
        <Loader2 className="h-8 w-8 animate-spin text-[#5c7cfa] dark:text-[#91a7ff]" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Profile Information Card */}
      <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-sm">
        <div className="p-6 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Profile Information
              </h2>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
                Update your personal information and avatar
              </p>
            </div>
            {!isEditing && (
              <button
                onClick={handleStartEdit}
                className="px-4 py-2 bg-[#5c7cfa] text-white rounded-lg hover:bg-[#4c6ef5] dark:hover:bg-[#5c7cfa] transition-colors"
              >
                Edit Profile
              </button>
            )}
          </div>
        </div>

        <div className="p-6">
          {/* Avatar Section */}
          <div className="flex items-center gap-6 mb-6 pb-6 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
            <div className="relative">
              {userData.avatarUrl ? (
                <img
                  src={userData.avatarUrl}
                  alt="Profile"
                  className="h-24 w-24 rounded-full object-cover"
                />
              ) : (
                <div className="h-24 w-24 rounded-full bg-blue-100 dark:bg-blue-900 flex items-center justify-center">
                  <User className="h-12 w-12 text-primary-500 dark:text-primary-300" />
                </div>
              )}
              {isEditing && (
                <label
                  htmlFor="avatar-upload"
                  className="absolute bottom-0 right-0 h-8 w-8 bg-[#5c7cfa] dark:bg-blue-700 rounded-full flex items-center justify-center cursor-pointer hover:bg-[#4c6ef5] dark:hover:bg-[#5c7cfa] transition-colors"
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
              <h3 className="text-lg font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                {userData.firstName} {userData.lastName}
              </h3>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                {userData.email}
              </p>
            </div>
          </div>

          {/* Profile Form */}
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                  First Name
                </label>
                <input
                  type="text"
                  value={userData.firstName}
                  onChange={(e) =>
                    setUserData({ ...userData, firstName: e.target.value })
                  }
                  disabled={!isEditing}
                  className="w-full px-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] dark:bg-[#1e2130] dark:text-[#eef0f6] rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-[#f1f3f9] dark:bg-[#1e2130] dark:disabled:bg-[#3a3f54] disabled:cursor-not-allowed"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                  Last Name
                </label>
                <input
                  type="text"
                  value={userData.lastName}
                  onChange={(e) =>
                    setUserData({ ...userData, lastName: e.target.value })
                  }
                  disabled={!isEditing}
                  className="w-full px-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] dark:bg-[#1e2130] dark:text-[#eef0f6] rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-[#f1f3f9] dark:bg-[#1e2130] dark:disabled:bg-[#3a3f54] disabled:cursor-not-allowed"
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                Email Address
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                <input
                  type="email"
                  value={userData.email}
                  disabled
                  className="w-full pl-10 pr-3 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] rounded-lg bg-[#f1f3f9] dark:bg-[#1e2130] dark:text-[#8b90a8] cursor-not-allowed"
                />
              </div>
              <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] dark:text-[#5c6180] mt-1">
                Email address cannot be changed. Contact support if you need to
                update it.
              </p>
            </div>

            <div>
              <label className="block text-sm font-medium text-[#3d4463] dark:text-[#c4c8db] mb-1">
                Phone Number
              </label>
              <div className="relative">
                <Phone className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-[#9ca0b8] dark:text-[#5c6180]" />
                <input
                  type="tel"
                  value={userData.phone}
                  onChange={(e) => {
                    setUserData({ ...userData, phone: e.target.value });
                    if (phoneError) setPhoneError('');
                  }}
                  disabled={!isEditing}
                  placeholder="+31612345678"
                  className={`w-full pl-10 pr-3 py-2 border dark:bg-[#1e2130] dark:text-[#eef0f6] rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent disabled:bg-[#f1f3f9] dark:disabled:bg-[#3a3f54] disabled:cursor-not-allowed ${
                    phoneError
                      ? 'border-red-500'
                      : 'border-[#c9cfd9] dark:border-[#3a3f54]'
                  }`}
                />
              </div>
              {phoneError ? (
                <p className="text-red-500 text-xs mt-1">{phoneError}</p>
              ) : (
                <p className="text-xs text-[#6b7194] dark:text-[#8b90a8] mt-1">
                  E.164 format required for SMS notifications (e.g.,
                  +31612345678)
                </p>
              )}
            </div>

            {isEditing && (
              <div className="flex justify-end gap-3 pt-4 border-t border-[#e2e6f0] dark:border-[#2a2e3f]">
                <button
                  onClick={handleCancelEdit}
                  className="px-4 py-2 border border-[#c9cfd9] dark:border-[#3a3f54] text-[#3d4463] dark:text-[#c4c8db] rounded-lg hover:bg-[#f1f3f9] dark:hover:bg-[#1e2130] transition-colors flex items-center gap-2"
                >
                  <X className="h-4 w-4" />
                  Cancel
                </button>
                <button
                  onClick={handleSaveProfile}
                  disabled={updateProfileMutation.isPending}
                  className="px-4 py-2 bg-[#5c7cfa] text-white rounded-lg hover:bg-[#4c6ef5] transition-colors flex items-center gap-2 disabled:opacity-50"
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
      <div className="bg-white dark:bg-[#14161f] rounded-lg shadow-sm">
        <div className="p-6 border-b border-[#e2e6f0] dark:border-[#2a2e3f]">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-[#1a1d2e] dark:text-[#eef0f6]">
                Security Settings
              </h2>
              <p className="text-sm text-[#6b7194] dark:text-[#8b90a8] mt-1">
                Manage your password and security preferences
              </p>
            </div>
          </div>
        </div>

        <div className="p-6">
          <div className="flex items-center justify-between p-4 bg-[#f8f9fc] dark:bg-[#0c0d14] dark:bg-[#1e2130] rounded-lg">
            <div className="flex items-center gap-3">
              <Lock className="h-5 w-5 text-[#6b7194] dark:text-[#8b90a8]" />
              <div>
                <p className="font-medium text-[#1a1d2e] dark:text-[#eef0f6]">
                  Password
                </p>
                <p className="text-sm text-[#6b7194] dark:text-[#8b90a8]">
                  Password changes are managed through your identity provider
                </p>
              </div>
            </div>
            <a
              href="/auth/change-password"
              className="px-4 py-2 bg-[#5c7cfa] text-white rounded-lg hover:bg-[#4c6ef5] transition-colors"
            >
              Change Password
            </a>
          </div>
        </div>
      </div>
    </div>
  );
};
