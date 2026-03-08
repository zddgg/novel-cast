import axios, { AxiosRequestHeaders, InternalAxiosRequestConfig } from 'axios';
import type { AxiosResponse } from 'axios';
import { Message } from '@arco-design/web-vue';

axios.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // let each request carry token
    // this example using the JWT token
    // Authorization is a custom headers key
    // please modify it according to the actual situation
    if (!config.headers) {
      config.headers = {} as AxiosRequestHeaders;
    }
    config.headers['Cache-Control'] = 'no-store,no-cache,must-revalidate';
    return config;
  },
  (error) => {
    // do something
    return Promise.reject(error);
  }
);
// add response interceptors
axios.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data;
    // if the custom code is not 20000, it is judged as an error.
    if (res.code !== '0000') {
      Message.error({
        content: res.msg || 'Error',
        duration: 5 * 1000,
      });
      return Promise.reject(new Error(res.msg || 'Error'));
    }
    return res;
  },
  (error) => {
    Message.error({
      content: error.msg || 'Request Error',
      duration: 5 * 1000,
    });
    return Promise.reject(error);
  }
);
