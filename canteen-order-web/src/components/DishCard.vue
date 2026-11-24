<template>
  <article class="dish-card" :class="{ sold: soldOut }">
    <div class="cover">
      <img
        v-if="menu.coverUrl"
        :src="assetUrl(menu.coverUrl)"
        :alt="menu.dishName"
        loading="lazy"
      />
      <div v-else class="cover-fallback">
        <span>{{ initialOf(menu.dishName) }}</span>
        <em>{{ menu.categoryName }}</em>
      </div>

      <div class="cover-tags">
        <span v-if="menu.sales >= 10" class="tag-hot">热销</span>
        <span v-if="lowStock" class="tag-low">仅剩 {{ menu.stockLeft }} 份</span>
      </div>

      <div v-if="soldOut" class="sold-mask">今日售罄</div>
    </div>

    <div class="body">
      <div class="meta">
        <span class="stall">{{ menu.stallName }}</span>
        <span class="dot"></span>
        <span>{{ menu.categoryName }}</span>
      </div>

      <h3>{{ menu.dishName }}</h3>

      <div class="foot">
        <div class="price">
          <small>¥</small>
          <strong>{{ Number(menu.price).toFixed(2) }}</strong>
          <span>已售 {{ menu.sales }}</span>
        </div>

        <button
          class="add-btn"
          type="button"
          :disabled="soldOut"
          :aria-label="`将${menu.dishName}加入购物车`"
          @click="emit('add', menu)"
        >
          <el-icon><Plus /></el-icon>
        </button>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import type { MenuVO } from '@/types'
import { assetUrl, initialOf } from '@/utils/format'

const props = defineProps<{ menu: MenuVO }>()
const emit = defineEmits<{ (e: 'add', menu: MenuVO): void }>()

const soldOut = computed(() => Number(props.menu.stockLeft) <= 0)
const lowStock = computed(() => !soldOut.value && Number(props.menu.stockLeft) <= 10)
</script>

<style scoped>
.dish-card {
  background: #fff;
  border: 1px solid rgba(232, 224, 211, 0.9);
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(47, 34, 18, 0.06);
  transition: transform 0.22s ease, box-shadow 0.22s ease;
  display: flex;
  flex-direction: column;
}

.dish-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 18px 38px rgba(47, 34, 18, 0.12);
}

.cover {
  position: relative;
  height: 158px;
  overflow: hidden;
  background: #efe7da;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.4s ease;
}

.dish-card:hover .cover img {
  transform: scale(1.04);
}

.cover-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background:
    radial-gradient(circle at 78% 18%, rgba(255, 255, 255, 0.5) 0 22%, transparent 23%),
    linear-gradient(135deg, #f6e6cf 0%, #edcfae 55%, #e3b98f 100%);
}

.cover-fallback span {
  font-family: 'STKaiti', 'KaiTi', 'Songti SC', serif;
  font-size: 52px;
  font-weight: 800;
  color: rgba(92, 54, 22, 0.72);
  line-height: 1;
}

.cover-fallback em {
  font-style: normal;
  font-size: 11px;
  letter-spacing: 0.18em;
  color: rgba(92, 54, 22, 0.55);
}

.cover-tags {
  position: absolute;
  top: 10px;
  left: 10px;
  display: flex;
  gap: 6px;
}

.tag-hot,
.tag-low {
  padding: 4px 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  backdrop-filter: blur(6px);
}

.tag-hot {
  color: #fff;
  background: rgba(216, 87, 42, 0.9);
}

.tag-low {
  color: #8a4a12;
  background: rgba(255, 242, 216, 0.94);
}

.sold-mask {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  background: rgba(29, 42, 36, 0.56);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.body {
  padding: 14px 16px 16px;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.meta {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 11px;
  color: var(--muted);
}

.stall {
  color: var(--accent);
  font-weight: 700;
}

.dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: #c9c0b2;
}

h3 {
  margin: 8px 0 14px;
  font-size: 16px;
  font-weight: 750;
  letter-spacing: -0.01em;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.foot {
  margin-top: auto;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
}

.price {
  display: flex;
  align-items: baseline;
  gap: 2px;
  color: var(--brand);
}

.price small {
  font-size: 12px;
  font-weight: 700;
}

.price strong {
  font-size: 24px;
  line-height: 1;
  font-weight: 850;
  letter-spacing: -0.03em;
  font-variant-numeric: tabular-nums;
}

.price span {
  margin-left: 6px;
  color: #a59b8c;
  font-size: 11px;
}

.add-btn {
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 50%;
  display: grid;
  place-items: center;
  color: #fff;
  background: var(--brand);
  box-shadow: 0 8px 18px rgba(216, 87, 42, 0.24);
  cursor: pointer;
  transition: transform 0.18s ease, background 0.18s ease;
}

.add-btn:hover:not(:disabled) {
  transform: scale(1.08);
  background: var(--brand-dark);
}

.add-btn:disabled {
  background: #cfc6b8;
  box-shadow: none;
  cursor: not-allowed;
}

.sold {
  opacity: 0.92;
}
</style>
