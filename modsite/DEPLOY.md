# Kurulum rehberi (VPS)

Bu rehber siteyi kendi sunucunda, kendi alan adınla ve ücretsiz HTTPS sertifikasıyla yayına almanı adım adım anlatır.
Toplam süre yaklaşık 30-45 dakika.

## 1. Gerekenler

| Ne | Öneri | Yaklaşık fiyat |
|---|---|---|
| Sunucu (VPS) | Hetzner CX22 (2 CPU, 4 GB RAM, 40 GB disk), Ubuntu 24.04 | ayda ~5 € |
| Alan adı | Namecheap, Porkbun, Cloudflare veya bir .com.tr kayıt firması | yılda ~10 $ |

Başlangıç için bu kadar yeter. Dosyalar büyüdükçe diski büyütebilir ya da Hetzner'den ek disk (Volume) takabilirsin.

## 2. Alan adını sunucuya yönlendir

Alan adını aldığın sitenin DNS ayarlarında iki kayıt ekle:

| Tür | Ad | Değer |
|---|---|---|
| A | `@` | sunucunun IP adresi |
| A | `www` | sunucunun IP adresi |

Yayılması birkaç dakika ile birkaç saat sürebilir.

## 3. Sunucuya bağlan ve Docker'ı kur

Windows'ta PowerShell veya Terminal aç:

```bash
ssh root@SUNUCU_IP
```

Sonra sunucuda:

```bash
apt update && apt upgrade -y
curl -fsSL https://get.docker.com | sh
ufw allow OpenSSH && ufw allow 80 && ufw allow 443 && ufw --force enable
```

## 4. Siteyi sunucuya kopyala

```bash
apt install -y git
git clone https://github.com/shdtdhdjtddtduddud-ui/krolasyon.git
cd krolasyon && git checkout claude/funny-hypatia-g4k221 && cd modsite
```

(Kendi deponu açarsan onun adresini kullan.)

## 5. Ayarları yap

```bash
cp .env.example .env
nano .env
```

Şunları kendi bilgilerinle değiştir:

- `DOMAIN=alanadin.com`
- `SITE_URL=https://alanadin.com`
- `SITE_NAME=` sitenin adı
- `CONTACT_EMAIL=` iletişim e-postan
- `SESSION_SECRET=` aşağıdaki komutun çıktısı:
  ```bash
  openssl rand -hex 32
  ```

Kaydetmek için `Ctrl+O`, `Enter`, çıkmak için `Ctrl+X`.

Veri klasörünü oluştur:

```bash
mkdir -p data && chown 1000:1000 data
```

## 6. Başlat

```bash
docker compose up -d --build
```

Birkaç dakika sonra `https://alanadin.com` açılır. HTTPS sertifikası otomatik alınır.

**Hemen kayıt ol.** Siteye kayıt olan **ilk kişi otomatik olarak yönetici** olur.

## 7. Reklamlar (Google AdSense)

1. Sitede birkaç mod ve içerik olduktan sonra https://adsense.google.com adresinden başvur. Gizlilik ve kullanım koşulları sayfaları hazır; AdSense bunları ister.
2. Onay gelince AdSense'in verdiği kodları **Yönetim → Site ayarları → Reklamlar** bölümüne yapıştır:
   - `<head>` kodu: AdSense'in ana `<script>` etiketi
   - Liste, kenar çubuğu ve mod sayfası alanları: AdSense'te oluşturduğun reklam birimlerinin kodları
   - `ads.txt`: AdSense'in verdiği satır (örnek: `google.com, pub-XXXXXXXX, DIRECT, f08c47fec0942fa0`)
3. Türkiye'deki ve AB'deki ziyaretçiler için AdSense panelinden **çerez onay mesajını (Privacy & messaging)** aç.

## 8. Her ay yapacakların

1. **İnceleme sırası:** Yeni modların dosyalarını indir, virüs taramasından geçir (ör. https://www.virustotal.com), sonra onayla veya sebebini yazıp reddet.
2. **Gelir dağıtımı:** Ay bitince AdSense'te o ayın gelirine bak. **Yönetim → Gelir dağıtımı** sayfasında ayı seç, tutarı yaz ve **Dağıt**'a bas. Yapımcı payı (varsayılan %75), görüntülenme ve indirme puanlarına göre otomatik dağıtılır.
3. **Ödemeler:** **Yönetim → Ödemeler** sayfasındaki talepleri PayPal, Papara veya IBAN ile öde. Sonra "Ödendi" olarak işaretle ve not alanına işlem numarasını yaz.
4. **Şikâyetler:** Telif hakkı şikâyetlerinde içeriği hızlıca gizle.

## 9. Yedekleme

Veritabanı ve yüklenen dosyalar `data/` klasöründe. Her gece otomatik yedek almak için:

```bash
crontab -e
```

En alta şu satırı ekle (yolu kendi klasörüne göre düzelt):

```
30 3 * * * cd /root/krolasyon/modsite && ./scripts/backup.sh >> backups.log 2>&1
```

Yedekleri ara sıra kendi bilgisayarına da indir:

```bash
scp root@SUNUCU_IP:/root/krolasyon/modsite/backups/*.tar.gz .
```

## 10. Güncelleme

```bash
cd /root/krolasyon/modsite
git pull
docker compose up -d --build
```

## Faydalı komutlar

| Komut | Ne yapar |
|---|---|
| `docker compose logs -f app` | Sitenin kayıtlarını canlı gösterir |
| `docker compose restart app` | Siteyi yeniden başlatır |
| `docker compose down` | Siteyi durdurur (veriler silinmez) |

## Yasal notlar

- **Hukuki kontrol:** Kullanım koşulları, gizlilik politikası ve içerik kuralları hazır şablonlardır. Gelir elde etmeye başlamadan önce bir avukata kontrol ettirmeni öneririm.
- **Vergi:** Reklam geliri ve yapımcılara yapılan ödemeler vergiye tabi olabilir. Bir mali müşavirle görüş; şahıs şirketi kurmak gerekebilir.
- **Telif hakkı:** Kullanıcıların yüklediği içerikler için telif hakkı şikâyetlerine hızlı yanıt vermen gerekir. Bunun için "Bildir" düğmesi ve iletişim e-postası var.
