// Remove all data from database
MATCH (n)
DETACH DELETE n;

//Load all data into database
LOAD CSV WITH HEADERS FROM file:///bench.csv AS row FIELDTERMINATOR ’,’,
MERGE (a:Node id:row.sourceNode,location:row.SourceLocation)
MERGE (b:Node id:row.targetNode,location:row.TargetLocation)
CREATE (a)−[r:Call]− >(b)
SET r = row,
r.Count = toInteger(row.Count),
r.targetSize = toInteger(row.targetSize),
r.sourceNode = row.sourceNode,
r.targetNode = row.targetNode
r.SourceLocation = row.SourceLocation,
r.TargetLocation = row.TargetLocation,
r.CallLocation = row.CallLocation;

//Add total number of function invocations and size to each node
MATCH ()−[r:Call]− >(b:Node)
WITH SUM(r.Count) as sum,b, AVG(r.targetSize) as size
SET b.inc = sum,
b.size = size;

//Add distinct number of incoming callNodes with unique callSites for each callTarget to each node
MATCH ()−[r:Call]− >(b:Node)
WITH b, count(DISTINCT r.CallLocation) as distinct
SET b.distinct = distinct;

//Return number of incoming calls and size of each function
MATCH ()−[r:Call]− >(b)
WITH SUM(r.Count) as count, r.TargetLocation as id, r.targetNode as TargetName,
AVG(r.targetSize) as size
RETURN count,id,TargetName,size;

//Find leaf nodes with a single incoming callNode and below a certain size
MATCH (a)
WHERE NOT (a)−[ ]− >() AND a.distinct < 2 + AND a.size < 30
RETURN DISTINCT a.id, a.inc,a.location
ORDER BY a.inc DESC
