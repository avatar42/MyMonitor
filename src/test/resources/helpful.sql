select * from items where itemName='BoltOTA';
select * from items where itemName='Router2';
select * from items where itemName='Etekcity';

update items set propVal='dea.monitor.checker.CheckTivo' where itemName='Bolt' and propName='class';
update items set propVal='tivo' where itemName='Bolt' and propName='broadcastType';

delete from items where id=2711;
