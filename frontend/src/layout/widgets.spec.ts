import { describe, expect, it, beforeEach } from 'vitest'
import { defineComponent } from 'vue'
import {
  listTopBarWidgets,
  registerTopBarWidget,
  unregisterTopBarWidget
} from './widgets'

/* eslint-disable vue/one-component-per-file */
const Stub = defineComponent({ render: () => null })
const Other = defineComponent({ render: () => null })

describe('topbar widget registry', () => {
  beforeEach(() => {
    listTopBarWidgets().forEach((w) => unregisterTopBarWidget(w.key))
  })

  it('registers widgets and lists them sorted by order', () => {
    registerTopBarWidget({ key: 'b', component: Stub, order: 20 })
    registerTopBarWidget({ key: 'a', component: Stub, order: 5 })
    registerTopBarWidget({ key: 'c', component: Stub })
    const list = listTopBarWidgets()
    expect(list.map((w) => w.key)).toEqual(['a', 'b', 'c'])
  })

  it('overwrites widget on duplicate key', () => {
    registerTopBarWidget({ key: 'k', component: Stub, order: 1 })
    registerTopBarWidget({ key: 'k', component: Other, order: 1 })
    expect(listTopBarWidgets()).toHaveLength(1)
    expect(listTopBarWidgets()[0].component).toBe(Other)
  })

  it('unregister removes widget', () => {
    registerTopBarWidget({ key: 'x', component: Stub })
    unregisterTopBarWidget('x')
    expect(listTopBarWidgets()).toHaveLength(0)
  })
})
