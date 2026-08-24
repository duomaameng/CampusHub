<script setup lang="ts">
import { AlertTriangle, X } from '@lucide/vue'
import { onBeforeUnmount, watch } from 'vue'

const props = defineProps<{
  open: boolean
  title: string
  description: string
  confirmText?: string
  cancelText?: string
  loading?: boolean
}>()

const emit = defineEmits<{ confirm: []; cancel: [] }>()

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && props.open && !props.loading) emit('cancel')
}

watch(() => props.open, (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
}, { immediate: true })

window.addEventListener('keydown', handleKeydown)
onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <Transition name="confirm-dialog">
      <div v-if="open" class="confirm-backdrop" role="presentation" @click.self="!loading && emit('cancel')">
        <section class="confirm-card" role="alertdialog" aria-modal="true" :aria-labelledby="'confirm-title'" :aria-describedby="'confirm-description'">
          <button class="confirm-close" type="button" aria-label="关闭确认弹窗" :disabled="loading" @click="emit('cancel')"><X /></button>
          <span class="confirm-icon"><AlertTriangle aria-hidden="true" /></span>
          <div class="confirm-copy">
            <p class="confirm-eyebrow">请确认操作</p>
            <h2 id="confirm-title">{{ title }}</h2>
            <p id="confirm-description">{{ description }}</p>
          </div>
          <div class="confirm-actions">
            <button class="confirm-cancel" type="button" :disabled="loading" @click="emit('cancel')">{{ cancelText || '取消' }}</button>
            <button class="confirm-danger" type="button" :disabled="loading" @click="emit('confirm')">{{ loading ? '处理中...' : confirmText || '确认' }}</button>
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.confirm-backdrop{position:fixed;inset:0;z-index:12000;display:grid;place-items:center;padding:24px;background:rgba(25,26,35,.72);backdrop-filter:blur(6px)}
.confirm-card{position:relative;width:min(480px,100%);padding:28px;border:3px solid #000;border-radius:28px;background:radial-gradient(circle at 100% 0,#ffb454 0 74px,transparent 75px),#fff;box-shadow:none;display:grid;grid-template-columns:auto 1fr;gap:18px}
.confirm-close{position:absolute;top:14px;right:14px;width:38px;height:38px;display:grid;place-items:center;border:2px solid #000;border-radius:50%;background:#fff;cursor:pointer}.confirm-close svg{width:18px}
.confirm-icon{width:56px;height:56px;display:grid;place-items:center;border:2px solid #000;border-radius:18px;background:#ffb454}.confirm-icon svg{width:28px;height:28px}
.confirm-copy{padding-right:34px}.confirm-eyebrow{margin-bottom:4px;font-size:11px;font-weight:900;letter-spacing:.12em;color:#b45309}.confirm-copy h2{font-size:24px;font-weight:900;color:#000}.confirm-copy>p:last-child{margin-top:10px;color:#4a4e5b;font-size:14px;font-weight:700;line-height:1.65}
.confirm-actions{grid-column:1/-1;display:flex;justify-content:flex-end;gap:12px;padding-top:6px}.confirm-actions button{min-width:112px;padding:11px 18px;border:2px solid #000;border-radius:14px;font-weight:900;cursor:pointer}.confirm-cancel{background:#fff;color:#000}.confirm-cancel:hover{background:#fff1df}.confirm-danger{background:#c1121f;color:#fff}.confirm-danger:hover:not(:disabled){background:#9f0f19;transform:translateY(-1px)}button:disabled{cursor:not-allowed;opacity:.62}
.confirm-dialog-enter-active,.confirm-dialog-leave-active{transition:opacity .18s ease}.confirm-dialog-enter-active .confirm-card,.confirm-dialog-leave-active .confirm-card{transition:transform .18s ease,opacity .18s ease}.confirm-dialog-enter-from,.confirm-dialog-leave-to{opacity:0}.confirm-dialog-enter-from .confirm-card,.confirm-dialog-leave-to .confirm-card{opacity:0;transform:translateY(14px) scale(.97)}
@media(max-width:560px){.confirm-card{padding:22px;grid-template-columns:1fr;box-shadow:none}.confirm-icon{width:48px;height:48px}.confirm-actions{grid-column:1;display:grid;grid-template-columns:1fr 1fr}.confirm-copy{padding-right:0}}
</style>
