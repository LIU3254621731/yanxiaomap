<template>
  <div class="compliance-view">
    <!-- 返回按钮 -->
    <div class="back-bar">
      <div class="container">
        <el-button
          type="text"
          icon="arrow-left"
          @click="goBack"
        >
          返回
        </el-button>
      </div>
    </div>

    <!-- 合规声明内容 -->
    <div class="content">
      <div class="container">
        <el-card class="compliance-card">
          <template #header>
            <h1>合规声明</h1>
          </template>

          <div class="sections">
            <!-- 免责声明 -->
            <section class="section">
              <h2>免责声明</h2>
              <div class="section-content">
                <p>本网站（以下简称"本站"）提供的所有信息仅供参考，不构成任何形式的招生建议、承诺或保证。用户在参考本站信息时，应以各招生单位官方发布的最新招生简章、专业目录、复试分数线等文件为准。</p>
                <p>本站不对信息的准确性、完整性、及时性做出任何保证，也不对因使用本站信息而导致的任何损失承担责任。用户应独立判断并承担使用本站信息的一切风险。</p>
              </div>
            </section>

            <!-- 数据来源 -->
            <section class="section">
              <h2>数据来源</h2>
              <div class="section-content">
                <p>本站数据来源于以下官方或公开渠道：</p>
                <ul>
                  <li>中国研究生招生信息网（研招网）公布的历年招生数据</li>
                  <li>各高校研究生院官方网站发布的招生简章和专业目录</li>
                  <li>教育部、各省教育考试院发布的官方统计数据</li>
                  <li>经授权的第三方教育数据服务商提供的结构化数据</li>
                </ul>
                <p>本站承诺所有数据均通过合法渠道获取，并在法律允许的范围内使用。如数据提供方对数据使用有特殊要求，本站将严格遵守相关约定。</p>
              </div>
            </section>

            <!-- 备案信息 -->
            <section class="section">
              <h2>备案信息</h2>
              <div class="section-content">
                <div class="record-info">
                  <div class="info-item">
                    <span class="label">网站名称：</span>
                    <span class="value">考研院校地图择校网站</span>
                  </div>
                  <div class="info-item">
                    <span class="label">备案号：</span>
                    <span class="value">京ICP备XXXXXX号</span>
                  </div>
                  <div class="info-item">
                    <span class="label">备案机关：</span>
                    <span class="value">北京市通信管理局</span>
                  </div>
                  <div class="info-item">
                    <span class="label">备案时间：</span>
                    <span class="value">2026年4月</span>
                  </div>
                </div>
                <p>本站已按照国家法律法规要求完成ICP备案，备案信息可在工业和信息化部ICP/IP地址/域名信息备案管理系统查询。</p>
              </div>
            </section>

            <!-- 版权信息 -->
            <section class="section">
              <h2>版权信息</h2>
              <div class="section-content">
                <p>本站所有内容，包括但不限于文字、图片、图表、数据、页面设计等，除特别注明外，均归本网站所有或已获得合法授权。</p>
                <p>未经本站书面许可，任何单位和个人不得以任何形式复制、转载、摘编、传播本站内容。如需使用本站内容，请联系我们获取授权。</p>
                <p>本站尊重并保护知识产权，如发现本站内容侵犯了您的合法权益，请及时联系我们，我们将第一时间进行处理。</p>
              </div>
            </section>

            <!-- 联系方式 -->
            <section class="section">
              <h2>联系方式</h2>
              <div class="section-content">
                <div class="contact-info">
                  <div class="info-item">
                    <span class="label">联系邮箱：</span>
                    <span class="value">contact@yanxiaomap.com</span>
                  </div>
                  <div class="info-item">
                    <span class="label">联系电话：</span>
                    <span class="value">010-XXXXXXX</span>
                  </div>
                  <div class="info-item">
                    <span class="label">办公地址：</span>
                    <span class="value">北京市海淀区XXXXXX</span>
                  </div>
                </div>
                <p>如果您对本合规声明有任何疑问或建议，欢迎通过以上方式与我们联系。</p>
              </div>
            </section>
          </div>

          <!-- 底部确认 -->
          <div class="footer-confirm">
            <p>最后更新日期：2026年4月17日</p>
            <p>继续使用本网站即表示您已阅读、理解并同意本合规声明的所有内容。</p>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goBack = () => {
  router.go(-1)
}
</script>

<style scoped lang="scss">
.compliance-view {
  min-height: 100vh;
  background: #f5f5f5;
}

.back-bar {
  background: white;
  padding: 16px 0;
  border-bottom: 1px solid #e8e8e8;

  .container {
    max-width: 1400px;
    margin: 0 auto;
    padding: 0 24px;
  }
}

.content {
  padding: 32px 0;

  .container {
    max-width: 1000px;
    margin: 0 auto;
    padding: 0 24px;
  }
}

.compliance-card {
  border-radius: 8px;
  overflow: hidden;

  :deep(.el-card__header) {
    background: #fafafa;
    border-bottom: 1px solid #e8e8e8;
    padding: 24px;

    h1 {
      margin: 0;
      font-size: 32px;
      color: #333;
      text-align: center;
    }
  }

  :deep(.el-card__body) {
    padding: 32px;
  }
}

.sections {
  .section {
    margin-bottom: 32px;
    padding-bottom: 32px;
    border-bottom: 1px solid #f0f0f0;

    &:last-child {
      margin-bottom: 0;
      padding-bottom: 0;
      border-bottom: none;
    }

    h2 {
      margin: 0 0 16px;
      font-size: 20px;
      color: #1890ff;
    }

    .section-content {
      p {
        margin: 12px 0;
        line-height: 1.6;
        color: #333;
      }

      ul {
        margin: 12px 0;
        padding-left: 20px;

        li {
          margin: 8px 0;
          line-height: 1.6;
          color: #333;
        }
      }

      .record-info,
      .contact-info {
        background: #fafafa;
        padding: 16px;
        border-radius: 4px;
        margin: 16px 0;

        .info-item {
          margin: 8px 0;

          .label {
            color: #666;
            margin-right: 8px;
          }

          .value {
            font-weight: 500;
            color: #333;
          }
        }
      }
    }
  }
}

.footer-confirm {
  margin-top: 40px;
  padding-top: 24px;
  border-top: 2px solid #e8e8e8;
  text-align: center;

  p {
    margin: 8px 0;
    color: #666;
    font-size: 14px;

    &:last-child {
      font-weight: 500;
      color: #333;
    }
  }
}
</style>