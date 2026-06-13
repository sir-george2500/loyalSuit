/**
 * Single source of truth for the Loyal Spare Parts brand — name, motto, and contact details
 * from the brand guideline. Import these instead of hard-coding the brand name in components.
 */
export const BRAND = {
  name: 'Loyal Spare Parts',
  legalName: 'LOYAL SPARE PARTS Ltd',
  motto: 'Trusted Parts. Loyal Service.',
  tagline: 'Quality automotive spare parts — verified, fairly priced, delivered nationwide.',
  description:
    'Loyal Spare Parts is Rwanda’s trusted marketplace for verified, high-quality automotive ' +
    'spare parts — simple to buy, fairly priced, and delivered nationwide.',
  email: 'loyalspareparts@gmail.com',
  phone: '+250 788 333 912',
  location: 'Nyabugogo, Kigali, Rwanda',
  url: 'www.loyalspareparts.com',
} as const
