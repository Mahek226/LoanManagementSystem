# Animation Implementation Guide

## Quick Reference for Adding Animations

### 1. Page Load Animations

```css
/* Fade in entire page */
.page-container {
  animation: fadeIn 0.6s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
```

### 2. Card Animations

```css
/* Staggered card entrance */
.card:nth-child(1) { animation: slideInUp 0.5s ease-out; }
.card:nth-child(2) { animation: slideInUp 0.6s ease-out; }
.card:nth-child(3) { animation: slideInUp 0.7s ease-out; }

@keyframes slideInUp {
  from {
    transform: translateY(30px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* Hover effect */
.card {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
}
```

### 3. Button Animations

```css
/* Ripple effect */
.btn {
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.btn::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  transform: translate(-50%, -50%);
  transition: width 0.6s, height 0.6s;
}

.btn:hover::before {
  width: 300px;
  height: 300px;
}

.btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
```

### 4. Input Focus Animations

```css
.form-input {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.form-input:focus {
  transform: translateY(-2px);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15);
  animation: inputPulse 0.5s ease-out;
}

@keyframes inputPulse {
  0% { box-shadow: 0 0 0 0 rgba(59, 130, 246, 0.4); }
  100% { box-shadow: 0 0 0 8px rgba(59, 130, 246, 0); }
}
```

### 5. Modal Animations

```css
.modal.fade .modal-dialog {
  transition: transform 0.3s ease-out;
  transform: scale(0.8);
}

.modal.show .modal-dialog {
  transform: scale(1);
}
```

### 6. Badge/Chip Animations

```css
.badge {
  animation: badgePop 0.5s ease-out;
  transition: all 0.3s ease;
}

@keyframes badgePop {
  0% { transform: scale(0); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.badge:hover {
  transform: scale(1.1);
}
```

### 7. Loading Animations

```css
/* Spinner */
.spinner {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Pulse */
.pulse {
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.8; transform: scale(1.05); }
}

/* Shimmer/Skeleton */
.skeleton {
  animation: shimmer 2s infinite linear;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 1000px 100%;
}

@keyframes shimmer {
  0% { background-position: -1000px 0; }
  100% { background-position: 1000px 0; }
}
```

### 8. List Item Animations

```css
.list-item {
  animation: slideInLeft 0.5s ease-out;
  animation-fill-mode: both;
}

.list-item:nth-child(1) { animation-delay: 0.1s; }
.list-item:nth-child(2) { animation-delay: 0.2s; }
.list-item:nth-child(3) { animation-delay: 0.3s; }

@keyframes slideInLeft {
  from {
    transform: translateX(-20px);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}
```

### 9. Icon Animations

```css
/* Bounce */
.icon-bounce {
  animation: iconBounce 2s ease-in-out infinite;
}

@keyframes iconBounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

/* Rotate on hover */
.icon-rotate {
  transition: transform 0.3s ease;
}

.icon-rotate:hover {
  transform: rotate(180deg);
}

/* Scale on hover */
.icon-scale {
  transition: transform 0.3s ease;
}

.icon-scale:hover {
  transform: scale(1.2);
}
```

### 10. Alert/Toast Animations

```css
.alert {
  animation: slideInDown 0.4s ease-out;
}

@keyframes slideInDown {
  from {
    transform: translateY(-20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* Auto-dismiss animation */
.alert.dismissing {
  animation: slideOutUp 0.3s ease-in forwards;
}

@keyframes slideOutUp {
  from {
    transform: translateY(0);
    opacity: 1;
  }
  to {
    transform: translateY(-20px);
    opacity: 0;
  }
}
```

## Best Practices

### Timing Functions
- **ease-out**: For entrances (elements coming into view)
- **ease-in**: For exits (elements leaving view)
- **ease-in-out**: For continuous animations
- **cubic-bezier(0.4, 0, 0.2, 1)**: Material Design standard

### Duration Guidelines
- **Micro-interactions**: 100-200ms (button clicks, toggles)
- **Small elements**: 200-300ms (badges, icons)
- **Medium elements**: 300-500ms (cards, modals)
- **Large elements**: 500-800ms (page transitions)
- **Infinite animations**: 2-3s (pulse, float effects)

### Performance Tips
1. **Use transform and opacity**: GPU-accelerated
2. **Avoid animating**: width, height, top, left, margin, padding
3. **Use will-change**: For complex animations (sparingly)
4. **Reduce on mobile**: Fewer animations for better performance
5. **Test at 60fps**: Use Chrome DevTools Performance tab

### Accessibility
```css
/* Respect user preferences */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
```

## Common Animation Patterns

### 1. Staggered List
```css
.item {
  animation: fadeInUp 0.5s ease-out;
  animation-fill-mode: both;
}

.item:nth-child(1) { animation-delay: 0.1s; }
.item:nth-child(2) { animation-delay: 0.2s; }
.item:nth-child(3) { animation-delay: 0.3s; }
/* ... continue for more items */
```

### 2. Hover Lift
```css
.element {
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.element:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}
```

### 3. Pulse Attention
```css
.attention {
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}
```

### 4. Shake Error
```css
.error-shake {
  animation: shake 0.5s ease-in-out;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-10px); }
  75% { transform: translateX(10px); }
}
```

### 5. Fade Scale
```css
.fade-scale {
  animation: fadeScale 0.5s ease-out;
}

@keyframes fadeScale {
  from {
    opacity: 0;
    transform: scale(0.8);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}
```

## Usage in Components

### Angular Component
```typescript
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-example',
  template: `
    <div class="animated-container" [@fadeIn]>
      <div class="card" *ngFor="let item of items; let i = index"
           [style.animation-delay]="(i * 0.1) + 's'">
        {{ item }}
      </div>
    </div>
  `,
  styles: [`
    .animated-container {
      animation: fadeIn 0.6s ease-out;
    }
    
    .card {
      animation: slideInUp 0.5s ease-out;
      animation-fill-mode: both;
    }
  `]
})
export class ExampleComponent implements OnInit {
  items = ['Item 1', 'Item 2', 'Item 3'];
  
  ngOnInit() {
    // Component logic
  }
}
```

## Debugging Animations

### Chrome DevTools
1. Open DevTools (F12)
2. Go to "More tools" > "Animations"
3. Trigger animation
4. Inspect timing, duration, easing

### Firefox DevTools
1. Open DevTools (F12)
2. Go to Inspector tab
3. Click animation icon next to element
4. View animation timeline

## Resources

- [MDN CSS Animations](https://developer.mozilla.org/en-US/docs/Web/CSS/CSS_Animations)
- [Cubic Bezier Generator](https://cubic-bezier.com/)
- [Animista](https://animista.net/) - CSS animation library
- [Animate.css](https://animate.style/) - Ready-to-use animations
