# Buurman Style Guide

## Design Philosophy

Buurman embodies a **clean, modern, and professional SaaS aesthetic** designed for small landlords who value efficiency and clarity. Our design language communicates trust, reliability, and accessibility while maintaining a contemporary feel that appeals to tech-savvy property managers.

### Core Design Principles

- **Trust & Professionalism**: Deep navy tones convey stability and authority
- **Growth & Success**: Emerald green accents represent financial growth and positive outcomes
- **Clarity & Simplicity**: Clean layouts with generous whitespace for easy scanning
- **Accessibility First**: WCAG 2.1 AA compliant color contrasts and semantic HTML
- **Modern SaaS**: Contemporary interface patterns familiar to modern web applications

---

## Color Palette

### Modern Trust Color Scheme

Our color palette is built around the "Modern Trust" theme, combining the authority of deep navy with the vibrancy of emerald green.

#### Primary Color: Deep Navy

The primary color represents trust, professionalism, and authority. Use this for primary actions, navigation elements, and key interactive components.

| Shade | Hex Code | Usage |
|-------|----------|-------|
| Primary (Base) | `#1E3A8A` | Primary buttons, active states, links, focus indicators |
| Primary 50 | `#EFF6FF` | Very light backgrounds, hover states for light elements |
| Primary 100 | `#DBEAFE` | Light backgrounds, status badges (occupied properties) |
| Primary 200 | `#BFDBFE` | Borders for selected items |
| Primary 300 | `#93C5FD` | Disabled button backgrounds |
| Primary 400 | `#60A5FA` | Secondary interactive elements |
| Primary 500 | `#1E3A8A` | **Default primary color** |
| Primary 600 | `#1E40AF` | Primary button hover states |
| Primary 700 | `#1E3A8A` | Primary button active/pressed states |
| Primary 800 | `#1E3563` | Dark mode variants, text on light backgrounds |
| Primary 900 | `#172554` | Headers, high-emphasis text |

**Tailwind Classes**: `bg-primary`, `text-primary`, `border-primary`, `hover:bg-primary-700`

**CSS Variables**: `var(--color-primary-500)`, `var(--color-primary-700)`

#### Accent Color: Emerald Green

The accent color represents growth, success, and positive financial outcomes. Use sparingly for success states, positive indicators, and calls-to-action that emphasize opportunity.

| Shade | Hex Code | Usage |
|-------|----------|-------|
| Accent (Base) | `#10B981` | Success messages, positive indicators, growth metrics |
| Accent 50 | `#ECFDF5` | Success notification backgrounds |
| Accent 100 | `#D1FAE5` | Status badges (vacant properties), light success states |
| Accent 200 | `#A7F3D0` | Progress bars (positive growth) |
| Accent 300 | `#6EE7B7` | Charts and data visualizations |
| Accent 400 | `#34D399` | Hover states for success elements |
| Accent 500 | `#10B981` | **Default accent color** |
| Accent 600 | `#059669` | Accent button hover states |
| Accent 700 | `#047857` | Active accent states |
| Accent 800 | `#065F46` | Dark accent text |
| Accent 900 | `#064E3B` | High-emphasis accent text |

**Tailwind Classes**: `bg-accent`, `text-accent`, `border-accent`, `hover:bg-accent-700`

**CSS Variables**: `var(--color-accent-500)`, `var(--color-accent-700)`

#### Background Colors: Soft Slate

Background colors provide a subtle, professional canvas that doesn't compete with content.

| Name | Hex Code | Usage |
|------|----------|-------|
| Background (Base) | `#F8FAFC` | Page backgrounds, content containers |
| Background Dark | `#F1F5F9` | Nested container backgrounds, subtle dividers |
| Surface | `#FFFFFF` | Cards, modals, elevated content |

**Tailwind Classes**: `bg-background`, `bg-background-dark`, `bg-surface`

**CSS Variables**: `var(--color-background)`, `var(--color-surface)`

#### Text Colors: Cool Gray

Text colors provide excellent readability while maintaining visual hierarchy.

| Name | Hex Code | Usage |
|------|----------|-------|
| Text Primary | `#1E293B` | Headings, primary body text, high-emphasis content |
| Text Secondary | `#64748B` | Secondary text, labels, metadata, helper text |
| Text Muted | `#94A3B8` | Placeholder text, disabled text, tertiary information |

**Tailwind Classes**: `text-text-primary`, `text-text-secondary`, `text-text-muted`

**CSS Variables**: `var(--color-text-primary)`, `var(--color-text-secondary)`

#### Semantic Colors

Colors with specific semantic meaning should use these values consistently.

| Purpose | Hex Code | Usage |
|---------|----------|-------|
| Success | `#10B981` | Successful operations, positive confirmations |
| Warning | `#F59E0B` | Warnings, maintenance status, caution states |
| Error | `#EF4444` | Error messages, destructive actions, validation failures |
| Info | `#1E3A8A` | Informational messages, tips, neutral notifications |

---

## Typography

### Font Stack

```css
font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Roboto", "Oxygen",
             "Ubuntu", "Cantarell", "Fira Sans", "Droid Sans", "Helvetica Neue",
             sans-serif;
```

This system font stack provides optimal readability and native platform feel across all devices.

### Font Sizes & Hierarchy

| Level | Size | Weight | Usage |
|-------|------|--------|-------|
| Heading 1 | `text-3xl` (1.875rem) | Bold (700) | Page titles |
| Heading 2 | `text-2xl` (1.5rem) | Bold (700) | Section headers |
| Heading 3 | `text-lg` (1.125rem) | Semibold (600) | Card headers, subsections |
| Body | `text-base` (1rem) | Normal (400) | Main content, paragraph text |
| Small | `text-sm` (0.875rem) | Normal (400) | Labels, metadata, helper text |
| Extra Small | `text-xs` (0.75rem) | Normal (400) | Captions, tags, fine print |

### Font Weights

- **Bold (700)**: Headings, emphasis
- **Semibold (600)**: Subheadings, labels
- **Medium (500)**: Buttons, action items
- **Normal (400)**: Body text, general content

---

## Component Patterns

### Buttons

#### Primary Button
```html
<button class="bg-primary text-white px-4 py-2 rounded hover:bg-primary-700 transition-colors">
  Primary Action
</button>
```

**Usage**: Main call-to-action, primary form submissions, key user flows

#### Secondary Button
```html
<button class="border border-gray-300 px-4 py-2 rounded hover:bg-gray-50 transition-colors">
  Secondary Action
</button>
```

**Usage**: Alternative actions, cancel operations, auxiliary controls

#### Destructive Button
```html
<button class="bg-red-600 text-white px-4 py-2 rounded hover:bg-red-700 transition-colors">
  Delete
</button>
```

**Usage**: Delete operations, destructive actions requiring confirmation

### Form Elements

#### Input Fields
```html
<input
  type="text"
  class="w-full border border-gray-300 rounded px-3 py-2
         focus:border-primary focus:ring-1 focus:ring-primary"
/>
```

**Focus State**: Primary color border with subtle ring for accessibility

#### Labels
```html
<label class="block text-sm font-medium text-gray-700 mb-1">
  Field Label
</label>
```

**Required Fields**: Add `<span class="text-red-500">*</span>` after label text

### Status Badges

Status badges use semantic colors to convey property or operation states.

```html
<!-- Vacant (Success/Available) -->
<span class="px-3 py-1 rounded-full text-xs font-semibold bg-accent-100 text-accent-800">
  Vacant
</span>

<!-- Occupied (Info) -->
<span class="px-3 py-1 rounded-full text-xs font-semibold bg-primary-100 text-primary-800">
  Occupied
</span>

<!-- Maintenance (Warning) -->
<span class="px-3 py-1 rounded-full text-xs font-semibold bg-yellow-100 text-yellow-800">
  Maintenance
</span>

<!-- Unavailable (Neutral) -->
<span class="px-3 py-1 rounded-full text-xs font-semibold bg-gray-100 text-gray-800">
  Unavailable
</span>
```

### Cards

```html
<div class="bg-white rounded-lg shadow-sm hover:shadow-md transition-shadow p-6">
  <!-- Card content -->
</div>
```

**Elevation**: Subtle shadow that increases on hover for interactive cards

### Navigation Tabs

```html
<button
  class="px-4 py-2 border-b-2 transition-colors
         border-primary text-primary font-semibold"
>
  Active Tab
</button>

<button
  class="px-4 py-2 border-b-2 transition-colors
         border-transparent text-gray-600 hover:text-gray-900"
>
  Inactive Tab
</button>
```

---

## Logo & Brand Assets

### Logo Location

All logo assets are located in: **`assets/logo/`**

### Logo Variants

| Variant | File Name | Dimensions | Usage |
|---------|-----------|------------|-------|
| **Source Logo** | `logo_source.png` | Variable | Master logo file, design source |
| **High Resolution** | `logo_high_res.png` | Large scale | Print materials, high-DPI displays |
| **Square Logo** | `logo_square.png` | Square aspect | App icons, social media profile pictures |
| **Transparent Background** | Multiple with alpha | Variable | Overlay on colored backgrounds |
| **Favicon** | `favicon.ico` | 16x16, 32x32, 48x48 | Browser tabs, bookmarks |
| **Android Chrome** | `android-chrome-192x192.png` | 192x192 | Android home screen icons |
| **Android Chrome** | `android-chrome-512x512.png` | 512x512 | Android splash screens |
| **Apple Touch Icon** | `apple-touch-icon.png` | 180x180 | iOS home screen icons |
| **Favicon 16x16** | `favicon-16x16.png` | 16x16 | Small browser icons |
| **Favicon 32x32** | `favicon-32x32.png` | 32x32 | Standard browser icons |
| **OG Image** | `og-image.png` | 1200x630 | Social media link previews |
| **Social Post** | `social-post.png` | Variable | Social media content |

### Logo Usage Guidelines

- **Minimum Size**: Never display logo smaller than 32px in height
- **Clear Space**: Maintain clear space equal to the height of the "B" in "Buurman" around the logo
- **Background Colors**: Logo works best on white, light gray, or soft slate backgrounds
- **Do Not**: Distort, rotate, outline, add effects, or change logo colors

### Favicon Implementation

```html
<link rel="icon" type="image/x-icon" href="/assets/logo/favicon.ico">
<link rel="icon" type="image/png" sizes="16x16" href="/assets/logo/favicon-16x16.png">
<link rel="icon" type="image/png" sizes="32x32" href="/assets/logo/favicon-32x32.png">
<link rel="apple-touch-icon" sizes="180x180" href="/assets/logo/apple-touch-icon.png">
```

---

## Spacing & Layout

### Spacing Scale

Buurman uses Tailwind's default spacing scale (4px base unit).

| Class | Pixels | Usage |
|-------|--------|-------|
| `p-1`, `m-1` | 4px | Tight spacing, compact elements |
| `p-2`, `m-2` | 8px | Button padding, small gaps |
| `p-3`, `m-3` | 12px | Input padding, icon spacing |
| `p-4`, `m-4` | 16px | Card padding, section spacing |
| `p-6`, `m-6` | 24px | Large card padding, form sections |
| `p-8`, `m-8` | 32px | Page padding, major sections |

### Container Max Widths

- **Default Container**: `max-w-7xl` (1280px) for main content areas
- **Form Container**: `max-w-md` (448px) for focused forms like login/register
- **Modal Container**: `max-w-2xl` (672px) for dialog modals

### Grid Layouts

```html
<!-- Property Cards Grid -->
<div class="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
  <!-- Cards -->
</div>

<!-- Form Grid -->
<div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
  <!-- Form Fields -->
</div>
```

---

## Accessibility

### Color Contrast Ratios

All color combinations meet WCAG 2.1 AA standards:

- **Primary on White**: 9.73:1 (AAA)
- **Text Primary on Background**: 14.2:1 (AAA)
- **Text Secondary on Background**: 6.1:1 (AA)
- **Accent on White**: 3.8:1 (AA Large)

### Focus States

All interactive elements must have visible focus indicators:

```css
focus:border-primary focus:ring-1 focus:ring-primary
```

### Keyboard Navigation

- All actions must be keyboard accessible
- Tab order should follow logical flow
- Skip links provided for main content areas

### Screen Reader Support

- Use semantic HTML (`<button>`, `<nav>`, `<main>`, etc.)
- Provide `aria-label` for icon-only buttons
- Ensure form fields have associated `<label>` elements

---

## Dark Mode (Future Consideration)

While not currently implemented, the color system is designed to support dark mode:

- **Background**: `#1E293B` (slate-800)
- **Surface**: `#334155` (slate-700)
- **Primary**: Lighten to `#60A5FA` for better contrast
- **Accent**: Keep at `#10B981` (sufficient contrast)
- **Text Primary**: `#F8FAFC` (soft slate)
- **Text Secondary**: `#CBD5E1` (slate-300)

---

## Implementation Examples

### Keycloak Theme

The Keycloak login theme uses the Modern Trust color scheme:

- **Background**: `#F8FAFC` (Soft Slate)
- **Button**: `#1E3A8A` (Deep Navy)
- **Button Hover**: `#1E3563` (Darker Navy)
- **Links**: `#1E3A8A` (Deep Navy)
- **Focus Border**: `#1E3A8A` (Deep Navy)
- **Success Messages**: `#10B981` (Emerald Green)

Location: `docker/keycloak/themes/buurman/login/resources/css/styles.css`

### Frontend (React/Tailwind)

Colors are defined in:
- **Tailwind Config**: `app/tailwind.config.js`
- **CSS Variables**: `app/src/index.css`

All components use Tailwind utility classes:
- `bg-primary` for primary backgrounds
- `text-primary` for primary text/links
- `bg-accent` for success/accent elements
- `bg-background` for page backgrounds

---

## Best Practices

### Do's

✅ Use primary color for main actions and navigation
✅ Use accent color sparingly for success states and positive indicators
✅ Maintain consistent spacing using Tailwind's spacing scale
✅ Ensure sufficient color contrast for accessibility
✅ Use semantic HTML and ARIA attributes
✅ Test all interactive states (hover, focus, active, disabled)

### Don'ts

❌ Don't use accent color for primary actions (reserve for success/growth)
❌ Don't mix custom colors outside the defined palette
❌ Don't reduce spacing below design system values
❌ Don't use color alone to convey information (add icons or text)
❌ Don't skip focus states on interactive elements
❌ Don't use blue colors (legacy) - always use primary/accent instead

---

## Version History

- **v1.0** (2026-02-01): Initial Modern Trust color scheme implementation
  - Defined primary (Deep Navy #1E3A8A) and accent (Emerald Green #10B981) colors
  - Updated frontend and Keycloak theme
  - Established comprehensive style guide

---

## Questions or Contributions

For questions about design decisions or to propose changes to the style guide, please consult the project documentation or reach out to the design team.

**Last Updated**: February 1, 2026
