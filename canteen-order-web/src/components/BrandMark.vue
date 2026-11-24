<template>
  <div class="brand-mark" :class="{ light, compact }">
    <div class="mark">
      <span>食</span>
      <i></i>
    </div>
    <div v-if="!compact" class="brand-copy">
      <strong>西工食堂</strong>
      <small>CAMPUS CANTEEN</small>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    light?: boolean
    compact?: boolean
  }>(),
  { light: false, compact: false }
)
</script>

<style scoped>
.brand-mark {
  display: inline-flex;
  align-items: center;
  gap: 11px;
  user-select: none;
}

.mark {
  position: relative;
  width: 42px;
  height: 42px;
  border-radius: 13px;
  display: grid;
  place-items: center;
  color: #fff;
  background: linear-gradient(145deg, #e76a38 0%, #c74920 100%);
  box-shadow: 0 8px 18px rgba(201, 73, 32, 0.28);
  overflow: hidden;
}

.mark span {
  position: relative;
  z-index: 2;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.02em;
  font-family: 'STKaiti', 'KaiTi', 'Songti SC', serif;
}

.mark i {
  position: absolute;
  right: -12px;
  bottom: -14px;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.16);
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-copy strong {
  font-size: 18px;
  font-weight: 850;
  letter-spacing: 0.04em;
  color: var(--ink);
}

.brand-copy small {
  font-size: 9px;
  letter-spacing: 0.18em;
  color: var(--muted);
}

.light .brand-copy strong {
  color: #fff;
}

.light .brand-copy small {
  color: rgba(255, 255, 255, 0.55);
}

.compact .mark {
  width: 38px;
  height: 38px;
}
</style>
