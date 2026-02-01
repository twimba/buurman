import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';

const LoginPage: React.FC = () => {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  React.useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard');
    }
  }, [isAuthenticated, navigate]);

  return (
    <div className="flex items-center justify-center min-h-screen bg-background">
      <div className="bg-white p-8 rounded-lg shadow-md w-96">
        <h1 className="text-2xl font-bold mb-6 text-center">Buurman</h1>
        <p className="text-text-secondary mb-6 text-center">
          Property management for small landlords
        </p>
        <button
          onClick={login}
          className="w-full bg-primary text-white py-2 px-4 rounded hover:bg-primary-700"
        >
          Login with Keycloak
        </button>
        <p className="mt-4 text-sm text-text-secondary text-center">
          Don't have an account?{' '}
          <a href="/register" className="text-primary hover:underline">
            Register
          </a>
        </p>
      </div>
    </div>
  );
};

export default LoginPage;
