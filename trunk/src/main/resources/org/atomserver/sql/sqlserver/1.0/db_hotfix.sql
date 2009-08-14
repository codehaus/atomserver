SELECT Workspace, COUNT(*) FROM (
SELECT Workspace, UpdateSeqNum
   FROM EntryStore
  GROUP BY workspace, UpdateSeqNum
HAVING COUNT(*) > 1
) myInnerQuery
GROUP BY Workspace

CREATE TABLE BadSeqNums(
	Workspace VARCHAR(20),
	UpdateSeqNum INT,
	PRIMARY KEY (Workspace, UpdateSeqNum));

CREATE TABLE BadCalendars(
	Id INT IDENTITY(1,1)PRIMARY KEY CLUSTERED,
     Workspace VARCHAR(20),
     Collection VARCHAR(20),
     EntryId VARCHAR(32),
     LanCode CHAR(2),
     CountryCode CHAR(2))

CREATE TABLE BadListings(
	Id INT IDENTITY(1,1)PRIMARY KEY CLUSTERED,
     Workspace VARCHAR(20),
     Collection VARCHAR(20),
     EntryId VARCHAR(32),
     LanCode CHAR(2),
     CountryCode CHAR(2))

INSERT INTO BadSeqNums(Workspace, UpdateSeqNum) SELECT Workspace, UpdateSeqNum
   FROM EntryStore
  GROUP BY workspace, UpdateSeqNum
HAVING COUNT(*) > 1
  ORDER BY workspace, UpdateSeqNum

SELECT COUNT(*)
   FROM BadSeqNums

INSERT INTO BadCalendars(Workspace, Collection, EntryId, LanCode,
CountryCode)
SELECT s.Workspace, s.Collection, s.EntryId, s.LanCode, s.CountryCode
   FROM EntryStore s
   JOIN BadSeqNums b
     ON s.Workspace = b.Workspace
    AND s.UpdateSeqNum = b.UpdateSeqNum
    AND b.Workspace = 'calendars'

INSERT INTO BadListings(Workspace, Collection, EntryId, LanCode,
CountryCode)
SELECT s.Workspace, s.Collection, s.EntryId, s.LanCode, s.CountryCode
   FROM EntryStore s
   JOIN BadSeqNums b
     ON s.Workspace = b.Workspace
    AND s.UpdateSeqNum = b.UpdateSeqNum
    AND b.Workspace = 'listings'

GO

UPDATE EntryStore
    SET UpdateSeqNum = b.Id + (SELECT SeqNum FROM WorkspaceSequence WHERE Workspace = 'listings')
   FROM EntryStore s
   JOIN BadListings b
     ON s.Workspace = b.Workspace
    AND s.Collection = b.Collection
    AND s.EntryId = b.EntryId
    AND s.LanCode = b.LanCode
    AND s.CountryCode = b.CountryCode

UPDATE EntryStore
    SET UpdateSeqNum = b.Id + (SELECT SeqNum FROM WorkspaceSequence WHERE Workspace = 'calendars')
   FROM EntryStore s
   JOIN BadCalendars b
     ON s.Workspace = b.Workspace
    AND s.Collection = b.Collection
    AND s.EntryId = b.EntryId
    AND s.LanCode = b.LanCode
    AND s.CountryCode = b.CountryCode

SELECT * FROM WorkspaceSequence

UPDATE WorkspaceSequence
    SET SeqNum = (SELECT MAX(UpdateSeqNum)
   FROM EntryStore WHERE WorkspaceSequence.Workspace =
EntryStore.Workspace)

SELECT * FROM WorkspaceSequence


SELECT Workspace, COUNT(*) FROM (
SELECT Workspace, UpdateSeqNum
   FROM EntryStore
  GROUP BY workspace, UpdateSeqNum
HAVING COUNT(*) > 1
) myInnerQuery
GROUP BY Workspace

DROP TABLE BadSeqNums
DROP TABLE BadCalendars
DROP TABLE BadListings

