# Figma MCP Spec

## Source

- Figma link: https://www.figma.com/design/NFMpUXEZhZuSSIBp8dAvGo/Untitled?node-id=0-1&p=f&t=KGALxymciLvRTM8J-0
- File key: `NFMpUXEZhZuSSIBp8dAvGo`
- Root node: `0:1`
- Main frame: `1:193`
- Main frame name: `Group 17`
- Main frame size: `1200 x 1722`

## Page Structure

1. Header
   - Node: `1:140`
   - Size: `1200 x 64`
   - Brand: `Auctions`
   - Nav: `Categories`, `For you`, `Trending`
   - Search placeholder: `Tim kiem trang suc, do dung ...`
   - Right actions: favorites, cart-like icon, user/avatar, username/login/signup states

2. Hero / For You
   - Node: `1:24`
   - Size: `1200 x 305`
   - Title: `For You`
   - Subtitle: `Personalized auctions based on your interests and bidding history`
   - Interest chips: `Jewellery`, `Watches`, `Antiques`, `Art`, `Collectibles`

3. Stats Summary
   - Node: `1:41`
   - Size: `1200 x 210`
   - Cards:
     - `Saved Auctions`: `12`
     - `Active Bids`: `5`
     - `Auto-Bids`: `3`

4. Product Grid
   - Node: `1:59`
   - Size: `1200 x 1143`
   - Layout: `3 columns x 2 rows`
   - Card size: `357 x 461`
   - Example titles:
     - `Affordable Silver & Laminated Objects Auction`
     - `Emeralds, Rubies & Sapphires Auction`
     - `Exclusive White Diamonds Auction`
     - `Coloured Gemstones Jewellery Auction`
     - `Unused Watches Auction`
     - `Ceramic Figurines Auction`

## Visual Tokens

- Primary blue: `#0033ff` and `#2a6df4`
- Text primary: `#262626`
- Text muted: `#8c8c8c`, `#999999`
- Border: `#e6e6e6`, `#bababa`
- Surface: `#ffffff`, `#f2f2f2`
- Radius:
  - Pills: `9999px`
  - Card/button/search: `8px`
  - Wishlist icon bg: `36px` circle

## Typography

- Brand:
  - Font: `Inria Serif`
  - Style: `Bold Italic`
  - Size: `26px`
- Hero title:
  - Font: `Inter`
  - Weight: `500`
  - Size: `48px`
  - Line height: `48px`
- Body/subtitle:
  - Font: `Inter`
  - Size: `18px`
  - Line height: `28px`
- Section labels / nav / card metadata:
  - Font: `Inter`
  - Sizes seen: `12px`, `14px`, `16px`, `18px`

## MCP Notes

- Preferred inspect node for implementation: `1:193`
- Metadata was read via Figma MCP from the design URL above.
- `get_design_context` returned React + Tailwind reference code and screenshot for node `1:193`.
- Asset URLs returned by MCP are temporary and expire after a limited time.

## Suggested MCP Calls

```text
get_metadata:
  fileKey = NFMpUXEZhZuSSIBp8dAvGo
  nodeId  = 0:1

get_design_context:
  fileKey = NFMpUXEZhZuSSIBp8dAvGo
  nodeId  = 1:193
```
