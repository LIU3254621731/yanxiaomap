# 并行开发指南

## 概述

本指南描述了多 Agent 如何在同一项目的不同部分上同时开展工作，同时通过共享状态和明确定义的接口保持协调一致。并行开发的核心是通过接口定义和 Mock 数据解除依赖阻塞。

## 并行开发原则

### 1. 接口先行
- 在开始编码前，先定义所有模块间的接口
- 接口定义应包括：方法、路径、参数、请求/响应格式、错误码
- 所有 Agent 确认接口定义后再开始开发

### 2. Mock 数据驱动
- 依赖未就绪时，使用 Mock 数据模拟接口响应
- Mock 数据结构必须与最终接口完全一致
- 提供开关机制，方便从 Mock 切换到真实实现

### 3. 持续协调
- 定期同步各 Agent 的进度和变更
- 接口变更时立即通知相关 Agent
- 共享状态文件作为单一事实来源

### 4. 渐进集成
- 分阶段集成，每个阶段都有明确的验证点
- 早期集成可能模块，逐步加入其他模块
- 每个集成阶段都有回滚能力

## 依赖图与并行策略

### 典型依赖关系图

```
T001 数据库设计 (数据库 Agent)
  │
  ├── T002 后端核心 API (后端 Agent)
  │     │
  │     ├── T003 前端 UI 开发 (前端 Agent)
  │     │     │
  │     │     └── T006 集成测试 (部署测试 Agent)
  │     │
  │     ├── T005 安全合规 (安全 Agent)
  │     │     │
  │     │     └── T006 集成测试 (部署测试 Agent)
  │     │
  │     └── T006 集成测试 (部署测试 Agent)
  │
  └── T004 数据初始化 (数据库 Agent)
        │
        └── T002 后端核心 API
```

### 并行执行策略

| 阶段 | 并行任务 | 依赖要求 | Mock 策略 |
|------|---------|---------|----------|
| 阶段 1 | T001 数据库 + T004 数据初始化 | 无 | 不需要 |
| 阶段 2 | T002 后端 API + T005 安全 | T001 | 后端使用真实数据库，安全使用 Mock 接口 |
| 阶段 3 | T003 前端开发 | T001 + T002（先使用 Mock） | 前端先使用 Mock 数据，后接入真实 API |
| 阶段 4 | T006 集成测试与部署 | 全部完成 | 切换为真实数据 |

## Mock 数据管理

### Mock 数据结构示例

```typescript
// 前端 Mock 数据 - 开发时使用
export const mockData = {
  users: [
    {
      id: 1,
      username: "test_user",
      email: "test@example.com",
      created_at: "2026-01-01T00:00:00Z"
    }
  ],
  items: [
    { id: 1, name: "示例项目A", status: "active" },
    { id: 2, name: "示例项目B", status: "inactive" }
  ]
};

// 获取数据函数 - 支持无缝切换
export async function fetchItems(): Promise<Item[]> {
  // 开发环境使用 Mock，生产环境调用真实 API
  if (process.env.NODE_ENV === 'development' && !isBackendReady()) {
    return Promise.resolve(mockData.items);
  }
  const response = await axios.get('/api/items');
  return response.data.data;
}

function isBackendReady(): boolean {
  return process.env.VITE_USE_REAL_API === 'true';
}
```

```java
// 后端 Mock 服务 - 数据库未就绪时使用
@Service
@ConditionalOnProperty(name = "mock.enabled", havingValue = "true")
public class DataServiceMock implements DataService {
    @Override
    public Result<List<DataDTO>> getDataList() {
        return Result.success(Arrays.asList(
            new DataDTO(1, "示例数据A", 100),
            new DataDTO(2, "示例数据B", 200)
        ));
    }
}

@Service
@ConditionalOnProperty(name = "mock.enabled", havingValue = "false")
public class DataServiceImpl implements DataService {
    @Autowired
    private DataMapper dataMapper;

    @Override
    public Result<List<DataDTO>> getDataList() {
        List<DataDTO> list = dataMapper.selectList(null);
        return Result.success(list);
    }
}
```

## 代码分支策略

### 推荐的分支模型

```
main (稳定发布分支)
  ├── develop (开发主线)
  │     ├── feature/database (数据库 Agent)
  │     ├── feature/backend  (后端 Agent)
  │     ├── feature/frontend (前端 Agent)
  │     ├── feature/security (安全 Agent)
  │     └── feature/deploy   (部署测试 Agent)
  └── release/v1.0 (发布分支)
```

### 合并流程

1. 各 Agent 在独立的 feature 分支上开发
2. 集成协调 Agent 定期将 feature 分支合并到 develop
3. 集成测试在 develop 分支上进行
4. 测试通过后合并到 main 分支

## 进度同步机制

### 定期同步节奏

| 时间 | 同步内容 | 参与方 |
|------|---------|--------|
| 每日开始 | 计划当日工作 | 所有 Agent |
| 每日结束 | 报告当日进展 | 所有 Agent |
| 每 4 小时 | 检查接口变更 | 前端 + 后端 |
| 每次完成 | 提交代码并报告 | 各 Agent 独立进行 |

### 同步模板

```markdown
Agent 名称：[Agent名称]
日期：2026-04-17

## 当日计划
1. [任务] - [预计内容]
2. [任务] - [预计内容]

## 当日进展
- ✅ 已完成：[完成的工作]
- 🔄 进行中：[当前工作，进度%]
- ⏳ 待开始：[未开始的工作]

## 问题/阻塞
- [问题描述]（状态：已解决/等待中/需要协调）

## 预计完成时间
- [预计时间]
```

## 开发环境管理

### 本地开发环境
每个 Agent 在本地搭建完整的开发环境：
- 使用 Docker Compose 统一管理服务依赖
- 共享配置模板，减少环境差异
- 环境变量通过 `.env.template` 统一管理

### 集成开发环境
集成协调 Agent 维护集成环境：
- 定期将各 Agent 代码合并到集成分支
- 运行集成测试
- 提供可访问的集成测试 URL
