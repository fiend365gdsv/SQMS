import axios from 'axios';

const API_BASE = process.env.REACT_APP_API_URL || 'http://localhost:8080';

const API = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' }
});

export default API;
export { API_BASE };
