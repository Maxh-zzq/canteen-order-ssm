import { defineStore } from 'pinia'
import { addCartItem, clearCart, getCart, removeCartItem, updateCartItem } from '@/api/cart'
import type { AddCartPayload, CartItemVO, CartVO, UpdateCartPayload } from '@/types'

interface CartState {
  items: CartItemVO[]
  totalQuantity: number
  totalAmount: number
  loading: boolean
  loaded: boolean
}

export const useCartStore = defineStore('cart', {
  state: (): CartState => ({
    items: [],
    totalQuantity: 0,
    totalAmount: 0,
    loading: false,
    loaded: false
  }),
  getters: {
    isEmpty: (state): boolean => state.items.length === 0,
    distinctCount: (state): number => state.items.length
  },
  actions: {
    applyCart(cart: CartVO) {
      this.items = cart.items || []
      this.totalQuantity = cart.totalQuantity || 0
      this.totalAmount = Number(cart.totalAmount || 0)
      this.loaded = true
    },
    async fetchCart(): Promise<CartVO> {
      this.loading = true
      try {
        const cart = await getCart()
        this.applyCart(cart)
        return cart
      } finally {
        this.loading = false
      }
    },
    async add(payload: AddCartPayload): Promise<CartVO> {
      const cart = await addCartItem(payload)
      this.applyCart(cart)
      return cart
    },
    async update(payload: UpdateCartPayload): Promise<CartVO> {
      const cart = await updateCartItem(payload)
      this.applyCart(cart)
      return cart
    },
    async remove(dishId: number, specId?: number | null): Promise<CartVO> {
      const cart = await removeCartItem(dishId, specId)
      this.applyCart(cart)
      return cart
    },
    async clear(): Promise<void> {
      await clearCart()
      this.items = []
      this.totalQuantity = 0
      this.totalAmount = 0
    },
    reset() {
      this.items = []
      this.totalQuantity = 0
      this.totalAmount = 0
      this.loaded = false
    }
  }
})
