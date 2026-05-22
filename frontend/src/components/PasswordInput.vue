<script setup lang="ts">
import { Eye, EyeOff } from '@lucide/vue'
import { computed, ref } from 'vue'

const props = defineProps<{
  id?: string
  name?: string
  autocomplete?: string
  placeholder?: string
  minlength?: number | string
  maxlength?: number | string
  required?: boolean
  disabled?: boolean
  autofocus?: boolean
}>()

const model = defineModel<string>({ default: '' })
const visible = ref(false)
const inputType = computed(() => (visible.value ? 'text' : 'password'))

function toggleVisibility() {
  visible.value = !visible.value
}
</script>

<template>
  <div class="password-input-wrapper">
    <input
      :id="props.id"
      v-model="model"
      :name="props.name"
      :type="inputType"
      :autocomplete="props.autocomplete"
      :placeholder="props.placeholder"
      :minlength="props.minlength"
      :maxlength="props.maxlength"
      :required="props.required"
      :disabled="props.disabled"
      :autofocus="props.autofocus"
    />
    <button
      class="password-visibility-button"
      type="button"
      :aria-label="visible ? '隐藏密码' : '显示密码'"
      :title="visible ? '隐藏密码' : '显示密码'"
      :disabled="props.disabled"
      @click="toggleVisibility"
    >
      <EyeOff v-if="visible" class="password-visibility-icon" aria-hidden="true" />
      <Eye v-else class="password-visibility-icon" aria-hidden="true" />
    </button>
  </div>
</template>
