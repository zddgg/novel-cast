import { AxiosRequestConfig } from 'axios';
import { HttpResponse } from '@/types/global';

declare module 'axios' {
  export interface AxiosInstance {
    <T = any>(config: AxiosRequestConfig): Promise<HttpResponse<T>>;

    request<T = any>(config: AxiosRequestConfig): Promise<HttpResponse<T>>;

    get<T = any>(
      url: string,
      config?: AxiosRequestConfig
    ): Promise<HttpResponse<T>>;

    delete<T = any>(
      url: string,
      config?: AxiosRequestConfig
    ): Promise<HttpResponse<T>>;

    head<T = any>(
      url: string,
      config?: AxiosRequestConfig
    ): Promise<HttpResponse<T>>;

    post<T = any>(
      url: string,
      data?: any,
      config?: AxiosRequestConfig
    ): Promise<HttpResponse<T>>;

    put<T = any>(
      url: string,
      data?: any,
      config?: AxiosRequestConfig
    ): Promise<HttpResponse<T>>;

    patch<T = any>(
      url: string,
      data?: any,
      config?: AxiosRequestConfig
    ): Promise<HttpResponse<T>>;
  }
}
