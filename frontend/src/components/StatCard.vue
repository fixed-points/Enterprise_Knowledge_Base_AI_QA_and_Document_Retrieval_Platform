<template>
  <div class="stat-card">
    <div class="stat-label">{{ label }}</div>
    <div class="stat-value">{{ value }}</div>
    <div class="stat-hint">{{ hint }}</div>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], required: true },
  hint: { type: String, default: '' }
})
</script>

<style scoped>
.stat-card {
  padding: 22px;
  border-radius: 24px;
  color: #123047;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(17, 33, 58, 0.08);
  box-shadow: 0 18px 50px rgba(17, 33, 58, 0.08);
  backdrop-filter: blur(16px);
}

.stat-label {
  font-size: 14px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #5c7488;
}

.stat-value {
  margin-top: 10px;
  font-size: 34px;
  font-weight: 700;
}

.stat-hint {
  margin-top: 8px;
  font-size: 13px;
  color: #627487;
}
</style>
