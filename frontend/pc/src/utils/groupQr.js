import QRCode from 'qrcode'

export const GROUP_QR_BASE = 'https://watermelonim.eu.cc/#/pages/contacts/group-join'

export function groupQrContent(groupId) {
  return `${GROUP_QR_BASE}?id=${encodeURIComponent(groupId || '')}`
}

export async function makeGroupQrDataUrl(groupId, size = 320) {
  return QRCode.toDataURL(groupQrContent(groupId), {
    width: size,
    margin: 1,
    errorCorrectionLevel: 'M',
    color: { dark: '#111111', light: '#ffffff' },
  })
}
