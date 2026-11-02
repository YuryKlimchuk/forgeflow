import Keycloak from "keycloak-js";

export type RealmKey = "users" | "admins";

const CONFIGS: Record<RealmKey, { url: string; realm: string; clientId: string }> = {
  users: {
    url: "http://localhost:8081",
    realm: "forgeflow-users",
    clientId: "forgeflow-frontend",
  },
  admins: {
    url: "http://localhost:8081",
    realm: "forgeflow-admins",
    clientId: "forgeflow-admin",
  },
};

let instance: Keycloak | null = null;

export async function login(realm: RealmKey): Promise<void> {
  instance = new Keycloak(CONFIGS[realm]);
  await instance.init({ onLoad: "login-required", pkceMethod: "S256" });
}

export async function logout(): Promise<void> {
  if (instance) {
    await instance.logout({ redirectUri: window.location.origin });
    instance = null;
  }
}

export async function getToken(): Promise<string | null> {
  if (!instance?.authenticated) {
    return null;
  }
  try {
    await instance.updateToken(30);
  } catch {
    return null;
  }
  return instance.token ?? null;
}

export function isAuthenticated(): boolean {
  return !!instance?.authenticated;
}
