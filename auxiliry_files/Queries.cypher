### Remove all data from database
MATCH (n)
DETACH DELETE n;

##Load all data into database (l6-l16).
##Create count of number of calls uptill some time (l17-l26).
LOAD CSV WITH HEADERS FROM 'file:///Final.csv' AS row FIELDTERMINATOR ';'
MERGE (a:Node {id:row.Source,location:row.SourceLocation,text:row.SourceText})
MERGE (b:Node {id:row.Target,location:row.TargetLocation,text:row.TargetText})
CREATE (a)-[r:Call]->(b)
SET r = row,
r.Count = toInteger(row.Count),
r.ObjectInstance = row.ObjectInstance,
r.Return = row.Return,
r.Arguments = row.Arguments,
r.Time = apoc.convert.fromJsonList(row.Time),
r.Source = row.Source,
r.Target = row.Target,
r.SourceLocation = row.SourceLocation,
r.TargetLocation = row.TargetLocation,
r.callLocation = row.callLocation;

##import for partial graph
LOAD CSV WITH HEADERS FROM 'file:///out2.csv' AS row FIELDTERMINATOR ','
MERGE (a:Node {id:row.Source,location:row.SourceLocation})
MERGE (b:Node {id:row.Target,location:row.TargetLocation})
CREATE (a)-[r:Call]->(b)
SET r = row,
r.Count = toInteger(row.Count);

##display 10 largest calls for period
MATCH (a)-[r:Call]->(b)
WHERE size([x IN r.Time WHERE 100000000 < x < 200000000])>0
RETURN SUM(r.Count),a.id,a.location,b.id,b.location
ORDER BY SUM(r.Count) DESC
LIMIT 10;

## Set incoming calls as node property for visualisation
MATCH ()-[r:Call]->(b:Node)
WITH SUM(r.Count) as sum,b
SET b.inc = sum;

##Print everything
MATCH (a)-[r:Call]->(b)
RETURN r.Count,a.id,b.id;

##Template to export smaller graph
WITH "

MATCH (a)-[r:Call]->(b)
RETURN r.Count as Count,
r.Time as Time,
r.Source as Source,
r.Target as Target,
r.SourceLocation as SourceLocation,
r.TargetLocation as TargetLocation;

" AS query
CALL apoc.export.csv.query(query, "smaller.csv", {})
YIELD file, source, format, nodes, relationships, properties, time, rows, batchSize, batches, done, data
RETURN file, source, format, nodes, relationships, properties, time, rows, batchSize, batches, done, data;

##Example for running in python
"MATCH ()-[r:Call]->(b) "
"WHERE any(x IN r.Time WHERE $beginTime < x < $endTime) "
"RETURN SUM(r.Count) as count,b.location as id "
"ORDER BY count DESC "
"LIMIT $topFunctions"

##Some usefull indexes
CREATE INDEX function FOR (n:Node) ON (n.id);
CREATE INDEX locations FOR (n:Node) ON (n.location);
CREATE INDEX nodes FOR (n:Node) ON (n.location,n.id);
CREATE INDEX relationships FOR ()-[r:Call]-() ON (r.Source,r.Target,r.SourceLocation,r.TargetLocation);

