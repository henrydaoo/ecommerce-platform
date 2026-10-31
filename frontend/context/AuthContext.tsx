"use client";

import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { api, ApiError, setAccessToken, type User } from "../lib/api";

type AuthContextValue = {
  user?: User;
  isLoading: boolean;
  error?: string;
  login: (
    email: string,
    password: string,
    register?: boolean,
    fullName?: string,
  ) => Promise<void>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User>();
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string>();

  useEffect(() => {
    let active = true;
    api.auth
      .refresh()
      .then((session) => {
        if (active && session) {
          setAccessToken(session.accessToken);
          setUser(session.user);
        }
      })
      .catch((cause: unknown) => {
        if (active) {
          setError(
            cause instanceof ApiError && cause.status === 401
              ? "Your saved session expired or was already used. Please sign in again."
              : cause instanceof Error
                ? cause.message
                : "Unable to restore your session.",
          );
        }
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  const login = async (
    email: string,
    password: string,
    register = false,
    fullName = "",
  ) => {
    const session = register
      ? await api.auth.register(email, password, fullName)
      : await api.auth.login(email, password);
    setAccessToken(session.accessToken);
    setUser(session.user);
    setError(undefined);
  };

  const logout = async () => {
    await api.auth.logout();
    setAccessToken();
    setUser(undefined);
    setError(undefined);
  };

  return (
    <AuthContext.Provider
      value={{ user, isLoading, error, login, logout }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
