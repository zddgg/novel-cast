import axios from 'axios';
import { FileItem } from '@arco-design/web-vue/es/upload/interfaces';

export default function fileUploadApi(
  data: FormData,
  config: {
    controller: AbortController;
    onUploadProgress?: (progressEvent: any) => void;
  }
) {
  return axios.post<FileItem>('/api/file/upload', data, config);
}
