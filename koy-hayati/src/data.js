// Oyun verileri: eşyalar, ekinler, hayvanlar, dükkânlar
export const DAYS_PER_SEASON = 7;
export const MINUTES_PER_REAL_SECOND = 2.4; // 1 oyun günü ≈ 10 dakika

export const CROPS = {
  havuc: { name: 'Havuç', icon: '🥕', seasons: [0, 2], days: 3, seedPrice: 10, price: 28, yield: [1, 2] },
  patates: { name: 'Patates', icon: '🥔', seasons: [0, 2], days: 4, seedPrice: 12, price: 36, yield: [2, 3] },
  bugday: { name: 'Buğday', icon: '🌾', seasons: [0, 1], days: 4, seedPrice: 8, price: 26, yield: [1, 2] },
  domates: { name: 'Domates', icon: '🍅', seasons: [0, 1], days: 5, seedPrice: 18, price: 22, yield: [2, 4], regrow: 3 },
  lahana: { name: 'Lahana', icon: '🥬', seasons: [0, 2], days: 5, seedPrice: 15, price: 52, yield: [1, 1] },
  misir: { name: 'Mısır', icon: '🌽', seasons: [1], days: 6, seedPrice: 22, price: 30, yield: [2, 3], regrow: 4 },
  karpuz: { name: 'Karpuz', icon: '🍉', seasons: [1], days: 7, seedPrice: 35, price: 150, yield: [1, 1] },
  kabak: { name: 'Bal Kabağı', icon: '🎃', seasons: [2], days: 6, seedPrice: 30, price: 130, yield: [1, 1] },
};

export const ITEMS = {
  // tohumlar otomatik eklenir
  yumurta: { name: 'Yumurta', icon: '🥚', price: 22, food: 8, cat: 'hayvansal' },
  kaz_yumurtasi: { name: 'Kaz Yumurtası', icon: '🪺', price: 50, food: 12, cat: 'hayvansal' },
  sut: { name: 'İnek Sütü', icon: '🥛', price: 48, food: 10, cat: 'hayvansal' },
  keci_sutu: { name: 'Keçi Sütü', icon: '🍼', price: 58, food: 10, cat: 'hayvansal' },
  yun: { name: 'Koyun Yünü', icon: '🧶', price: 120, cat: 'hayvansal' },
  tavsan_tuyu: { name: 'Tavşan Tüyü', icon: '🐇', price: 95, cat: 'hayvansal' },
  trufle: { name: 'Trüf Mantarı', icon: '🟤', price: 320, cat: 'hayvansal' },
  bogurtlen: { name: 'Böğürtlen', icon: '🫐', price: 16, food: 6, cat: 'doğa' },
  mantar: { name: 'Orman Mantarı', icon: '🍄', price: 34, food: 9, cat: 'doğa' },
  kekik: { name: 'Kekik Demeti', icon: '🌿', price: 28, cat: 'doğa' },
  elma: { name: 'Elma', icon: '🍎', price: 18, food: 7, cat: 'meyve' },
  seftali: { name: 'Şeftali', icon: '🍑', price: 24, food: 8, cat: 'meyve' },
  sazan: { name: 'Sazan', icon: '🐟', price: 45, food: 12, cat: 'balık' },
  levrek: { name: 'Levrek', icon: '🐟', price: 70, food: 14, cat: 'balık' },
  turna: { name: 'Turna Balığı', icon: '🐡', price: 140, food: 16, cat: 'balık' },
  alabalik: { name: 'Alabalık', icon: '🐠', price: 85, food: 14, cat: 'balık' },
  buz_baligi: { name: 'Kış Alabalığı', icon: '🐠', price: 160, food: 16, cat: 'balık' },
  odun: { name: 'Odun', icon: '🪵', price: 12, cat: 'malzeme' },
  saman: { name: 'Saman Balyası', icon: '🟨', price: 22, cat: 'malzeme' },
  yem: { name: 'Hayvan Yemi', icon: '🥫', price: 8, cat: 'malzeme' },
  gubre: { name: 'Gübre', icon: '🧪', price: 15, cat: 'malzeme' },
  un: { name: 'Un', icon: '🥣', price: 90, cat: 'mutfak' },
  ekmek: { name: 'Köy Ekmeği', icon: '🍞', price: 75, food: 30, cat: 'mutfak' },
  peynir: { name: 'Köy Peyniri', icon: '🧀', price: 150, food: 18, cat: 'mutfak' },
  recel: { name: 'Reçel', icon: '🍯', price: 110, food: 15, cat: 'mutfak' },
  corba: { name: 'Sebze Çorbası', icon: '🍲', price: 95, food: 45, cat: 'mutfak' },
  buket: { name: 'Kır Çiçeği Buketi', icon: '💐', price: 45, cat: 'doğa' },
};
for (const [k, c] of Object.entries(CROPS)) {
  ITEMS[k] = { name: c.name, icon: c.icon, price: c.price, food: Math.round(c.price / 4), cat: 'ürün' };
  ITEMS[k + '_t'] = { name: c.name + ' Tohumu', icon: '🌱', sub: c.icon, price: c.seedPrice, cat: 'tohum', crop: k };
}

export const SPECIES = {
  tavuk: { name: 'Tavuk', icon: '🐔', price: 220, area: 'coop', product: 'yumurta', every: 1, verb: 'Yumurtayı topla', winterFeed: 'yem' },
  horoz: { name: 'Horoz', icon: '🐓', price: 280, area: 'coop', product: null, winterFeed: 'yem', note: 'Tavukların yumurta şansını artırır' },
  kaz: { name: 'Kaz', icon: '🪿', price: 380, area: 'coop', product: 'kaz_yumurtasi', every: 2, verb: 'Kaz yumurtasını al', winterFeed: 'yem' },
  tavsan: { name: 'Tavşan', icon: '🐇', price: 300, area: 'coop', product: 'tavsan_tuyu', every: 3, verb: 'Tüyünü tara', winterFeed: 'yem' },
  inek: { name: 'İnek', icon: '🐄', price: 1500, area: 'pasture', product: 'sut', every: 1, verb: 'Sütünü sağ', winterFeed: 'saman' },
  boga: { name: 'Boğa', icon: '🐂', price: 1900, area: 'pasture', product: null, winterFeed: 'saman', note: 'Baharda yavru şansını artırır, değeri yüksektir' },
  at: { name: 'At', icon: '🐎', price: 2600, area: 'pasture', product: null, winterFeed: 'saman', note: 'Binilebilir; at arabası çekebilir' },
  koyun: { name: 'Koyun', icon: '🐑', price: 900, area: 'pasture', product: 'yun', every: 3, verb: 'Yününü kırk', winterFeed: 'saman' },
  keci: { name: 'Keçi', icon: '🐐', price: 800, area: 'pasture', product: 'keci_sutu', every: 1, verb: 'Sütünü sağ', winterFeed: 'saman' },
  domuz: { name: 'Domuz', icon: '🐖', price: 1000, area: 'pigpen', product: 'trufle', every: 2, verb: 'Trüf mantarı ara', winterFeed: 'yem', seasons: [0, 2] },
};

export const TOOLS = [
  { id: 'el', name: 'El', icon: '✋' },
  { id: 'capa', name: 'Çapa', icon: '⛏️' },
  { id: 'sulama', name: 'Sulama Kabı', icon: '🚿' },
  { id: 'tohum', name: 'Tohum', icon: '🌱' },
  { id: 'balta', name: 'Balta', icon: '🪓' },
  { id: 'olta', name: 'Olta', icon: '🎣' },
];

export const UPGRADES = {
  sulama2: { name: 'Bakır Sulama Kabı', icon: '🚿', price: 650, desc: 'Kapasite 40, tek seferde 3 kare sular.' },
  balta2: { name: 'Çelik Balta', icon: '🪓', price: 800, desc: 'Ağaçlar 2 vuruşta devrilir, daha az enerji.' },
  olta2: { name: 'Usta Oltası', icon: '🎣', price: 750, desc: 'Balıklar daha hızlı vurur, nadir balık şansı artar.' },
  canta: { name: 'Büyük Sırt Çantası', icon: '🎒', price: 900, desc: 'Daha fazla enerji: maks. enerji 130.' },
  kamyonet: { name: 'Kamyonet', icon: '🛻', price: 4500, desc: 'Hızlı ulaşım. Çiftliğe teslim edilir.' },
  kayik: { name: 'Yelkenli Kayık', icon: '⛵', price: 2400, desc: 'Gölde gez, açıkta daha iyi balık tut. İskeleye teslim.' },
  atarabasi: { name: 'At Arabası', icon: '🐴', price: 1600, desc: 'Atın varsa çiftlikte kullanabilirsin.' },
  sera: { name: 'Sera', icon: '🏡', price: 6000, desc: 'Kışın bile ekim yapabileceğin 15 karelik sera.' },
};

export const POTS = [
  { id: 'monstera', name: 'Deve Tabanı', price: 220 },
  { id: 'palm', name: 'Areka Palmiyesi', price: 260 },
  { id: 'cactus', name: 'Sütun Kaktüs', price: 120 },
  { id: 'opuntia', name: 'Kulak Kaktüs', price: 140 },
  { id: 'fern', name: 'Eğrelti Otu', price: 160 },
  { id: 'bonsai', name: 'Bonsai', price: 420 },
  { id: 'snake', name: 'Paşa Kılıcı', price: 150 },
  { id: 'caladium', name: 'Pembe Kaladyum', price: 240 },
  { id: 'olive', name: 'Zeytin Fidanı', price: 300 },
  { id: 'rosemary', name: 'Biberiye', price: 110 },
];

export const RECIPES = [
  { out: 'peynir', n: 1, need: { sut: 2 }, name: 'Peynir mayala' },
  { out: 'peynir', n: 1, need: { keci_sutu: 2 }, name: 'Keçi peyniri' },
  { out: 'ekmek', n: 2, need: { un: 1, yumurta: 1 }, name: 'Ekmek pişir' },
  { out: 'recel', n: 1, need: { bogurtlen: 4 }, name: 'Böğürtlen reçeli' },
  { out: 'recel', n: 1, need: { elma: 3 }, name: 'Elma reçeli' },
  { out: 'recel', n: 1, need: { seftali: 3 }, name: 'Şeftali reçeli' },
  { out: 'corba', n: 1, need: { patates: 1, havuc: 1, lahana: 1 }, name: 'Sebze çorbası' },
  { out: 'corba', n: 1, need: { mantar: 2, patates: 1 }, name: 'Mantar çorbası' },
  { out: 'buket', n: 1, need: { kekik: 2 }, name: 'Kır buketi bağla' },
];

export const SEASON_INFO = [
  {
    name: 'İlkbahar', icon: '🌸',
    plus: ['Ekinler %25 daha hızlı büyür', 'Sık yağmur tarlayı kendiliğinden sular', 'Hayvanlar yavrulayabilir', 'Kır çiçekleri ve kekik toplanır'],
    minus: ['Fırtınalı günler olabilir', 'Mısır, karpuz ekilemez'],
  },
  {
    name: 'Yaz', icon: '☀️',
    plus: ['Uzun günler (05:00–21:30)', 'Karpuz ve mısır zamanı', 'Şeftali hasadı, böğürtlen bol', 'Göl ılık: yüzebilirsin'],
    minus: ['Sıcak günlerde toprak çabuk kurur: sulamazsan ekin 2 günde kurur', 'Öğle sıcağında enerji hızlı düşer'],
  },
  {
    name: 'Sonbahar', icon: '🍂',
    plus: ['Hasat bereketi: ekstra ürün şansı', 'Elma ve bal kabağı zamanı', 'Ormanda mantar ve trüf', 'Odun fiyatı yükselir'],
    minus: ['Günler kısalır', 'Fırtına sık', 'Yapraklar dökülür, otlar sararır'],
  },
  {
    name: 'Kış', icon: '❄️',
    plus: ['Göl donar: buz üstünde yürü, nadir kış alabalığı tut', 'Odun, süt ve balık fiyatları en yüksek', 'Mutfak işleri için bol zaman'],
    minus: ['Toprak donar: dışarıda ekim yok (sera hariç)', 'Hayvanlar otlayamaz: saman/yem gerekir', 'Soğuk enerjini düşürür (evde soba ısıtır)', 'Kar yürüyüşü yavaşlatır, günler kısa'],
  },
];
