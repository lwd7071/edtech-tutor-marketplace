import axios from 'axios';

export const axiosClient = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add token
axiosClient.interceptors.request.use((config) => {
  // We will get token from localStorage or Zustand store later
  // and attach it here: config.headers.Authorization = `Bearer ${token}`
  return config;
});

// Response interceptor to handle envelope and errors
axiosClient.interceptors.response.use(
  (response) => {
    // If it has our standard envelope, we might want to unwrap it, 
    // but usually react-query expects the full data to parse errors/meta too.
    return response.data;
  },
  (error) => {
    // Handle 401, refresh token logic here
    return Promise.reject(error);
  }
);
