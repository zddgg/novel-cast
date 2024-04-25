export interface IFetchStreamOptions {
  url: string;
  requestInit: RequestInit;
  onMessage: (data: string[], index: number) => void;
  onDone?: () => void;
  onError?: (response: Response) => void;
  onTimeout?: () => void;
}

export class FetchStream {
  url: string;

  requestInit: RequestInit;

  onMessage: IFetchStreamOptions['onMessage'];

  onDone: IFetchStreamOptions['onDone'];

  onError: IFetchStreamOptions['onError'];

  onTimeout: IFetchStreamOptions['onTimeout'];

  controller: AbortController | null = null;

  timer = 0;

  constructor(options: IFetchStreamOptions) {
    this.url = options.url;
    this.requestInit = options.requestInit;
    this.onMessage = options.onMessage;
    this.onDone = options.onDone;
    this.onError = options.onError;
  }

  startRequest() {
    this.controller = new AbortController();
    this.timeout();

    fetch(this.url, {
      method: 'POST',
      signal: this.controller.signal,
      ...this.requestInit,
    })
      .then((response) => {
        clearTimeout(this.timer);
        if (response.status === 200 && response.body) {
          return response.body;
        }
        // fetch() 返回的 Promise 不会被标记为 reject，即使响应的 HTTP 状态码是 404 或 500
        return Promise.reject(response);
      })
      .then(async (readableStream) => {
        // 1. 创建 reader 读取流队列
        const reader = readableStream.getReader();
        // 2. 记录流队列中分块的索引
        let index = 0;

        const readNextChunk = () => {
          reader
            .read()
            .then(({ value, done }) => {
              if (done) {
                this.onDone?.(); // 流已结束
              } else {
                const dataText = new TextDecoder().decode(value);
                const data = dataText.split('\n\n').filter(Boolean);
                this.onMessage(data, (index += 1));
                readNextChunk(); // 继续读取下一块数据
              }
            })
            .catch((error) => {
              this.onError?.(error);
            });
        };

        readNextChunk();
      })
      .catch((response) => {
        // ... error 处理
        this.onError?.(response);
      });
  }

  abort() {
    if (this.controller) this.controller.abort();
  }

  timeout(time = 60000) {
    this.timer = window.setTimeout(() => {
      this.abort();
      this.onTimeout?.(); // 外部若传入了监听超时回调，类似 onmessage
    }, time);
  }
}
