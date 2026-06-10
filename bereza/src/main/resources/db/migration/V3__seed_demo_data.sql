-- Демо-данные. Безопасно: для prod миграцию можно отключить (-Dflyway.target=2)
-- BCrypt-хеш для пароля "demo1234" (cost=12).
-- Сгенерировано один раз заранее, чтобы не зависеть от libs в SQL.

INSERT INTO users (username, email, password_hash, display_name, role) VALUES
  ('admin',  'admin@bereza.local',  '$2a$12$bbR7E96B3ar03BfvJ.MZ3O2GYjQk5G5y7m/lW9eb1iIBZUuD0Kdte', 'Администратор',  'ADMIN'),
  ('tourist','tourist@bereza.local','$2a$12$bbR7E96B3ar03BfvJ.MZ3O2GYjQk5G5y7m/lW9eb1iIBZUuD0Kdte', 'Иван Турист',     'TOURIST'),
  ('guide',  'guide@bereza.local',  '$2a$12$bbR7E96B3ar03BfvJ.MZ3O2GYjQk5G5y7m/lW9eb1iIBZUuD0Kdte', 'Пётр Гид',        'GUIDE'),
  ('hotel',  'hotel@bereza.local',  '$2a$12$bbR7E96B3ar03BfvJ.MZ3O2GYjQk5G5y7m/lW9eb1iIBZUuD0Kdte', 'Отель Берёзка',   'HOTEL');

INSERT INTO hotels (owner_id, name, description, city, address, latitude, longitude, stars, price_per_night, rooms_available, photos, amenities)
SELECT u.id,
       'Отель «Берёзка»',
       'Уютный отель в центре Суздаля. 10 минут до Кремля.',
       'Суздаль',
       'ул. Ленина, 45',
       56.4181, 40.4486, 4, 6500.00, 12,
       ARRAY['/uploads/hotels/1/1.jpg', '/uploads/hotels/1/2.jpg'],
       ARRAY['WiFi', 'Завтрак', 'Парковка', 'Баня']
FROM users u WHERE u.username = 'hotel';

