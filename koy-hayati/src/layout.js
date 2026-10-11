// Dünya yerleşimi: önemli noktalar, yollar, nehir, göl
export const WORLD = 640;
export const HALF = WORLD / 2;
export const SEG = 256;
export const CELL = WORLD / SEG;
export const WATER_Y = 0;
export const FARM_Y = 2.0;
export const TOWN_Y = 3.0;
export const UPPER_WATER_Y = 26;

export const FARM = { x: 0, z: 0, r0: 82, r1: 112 };
export const TOWN = { x: 0, z: 215, r0: 52, r1: 78 };
export const LAKE = { x: 155, z: 40, r: 55 };
export const ISLAND = { x: 172, z: 52, r: 9 };
export const POOL = { x: 60, z: -149 };
export const WATERFALL_X = 60;
export const PEAK = { x: -165, z: -205 };
export const FOREST = { x: -165, z: 40 };
export const MEADOW = { x: -85, z: -70 };

export const RIVER = [[60, -145], [78, -110], [92, -75], [112, -30], [140, 12]];
export const UPPER_STREAM = [[60, -162.5], [60, -186], [57, -212]];

export const ROADS = [
  // çiftlik → kasaba
  [[2, -10], [3, 30], [2, 60], [7, 100], [-4, 150], [0, 192]],
  // çiftlik → göl iskelesi
  [[4, 10], [30, 14], [60, 16], [85, 24], [101, 32]],
  // çiftlik → şelale
  [[-6, -40], [10, -70], [32, -105], [46, -136]],
  // çiftlik → orman
  [[-20, 4], [-70, 12], [-120, 26], [-152, 32]],
  // orman → zirve patikası
  [[-152, 32], [-148, -20], [-160, -80], [-150, -130], [-160, -170], [-165, -199]],
];

// Haritada gösterilen ve keşfedilebilen yerler
export const PLACES = [
  { id: 'farm', name: 'Çiftliğin', x: 0, z: -10, r: 60 },
  { id: 'town', name: 'Kasaba', x: 0, z: 215, r: 45 },
  { id: 'lake', name: 'Ayna Göl', x: 130, z: 40, r: 45 },
  { id: 'island', name: 'Göl Adası', x: ISLAND.x, z: ISLAND.z, r: 10 },
  { id: 'waterfall', name: 'Gümüş Şelale', x: 56, z: -146, r: 22 },
  { id: 'peak', name: 'Kartal Tepesi', x: PEAK.x, z: PEAK.z, r: 14 },
  { id: 'forest', name: 'Çam Ormanı', x: FOREST.x, z: FOREST.z, r: 50 },
  { id: 'meadow', name: 'Çiçekli Yayla', x: MEADOW.x, z: MEADOW.z, r: 30 },
];
