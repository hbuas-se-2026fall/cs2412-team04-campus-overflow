# 限界上下文映射图
1. 身份与权限 Identity
2. 问答核心 Q&A
3. 内容治理 Moderation
4. 声誉 Reputation
5. 通知 Notification
6. 检索 Discovery

关系说明：问答核心调用通知（客户-供应商），内容治理调用声誉（客户-供应商），检索消费问答（客户-供应商），问答与身份通过 ACL 防腐层隔离，问答与治理共享内核（Shared Kernel）。
