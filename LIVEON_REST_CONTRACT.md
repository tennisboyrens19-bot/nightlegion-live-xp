# Live On REST contract (static extraction)

Generated from the exact vendored upstream `ClanMessagesPlugin.java`.

## `submitDropStats` (line 1927)

- HTTP verbs: `PUT`
- Path segments: `(none detected)`
- JSON fields written: `accountType`, `category`, `content`, `dinkAccountHash`, `embeds`, `extra`, `items`, `killCount`, `npcId`, `party`, `playerName`, `rarestProbability`, `seasonalWorld`, `source`, `tts`, `type`, `world`
- Request body expressions: `screenshotBytes)`

## `liveOnAccountHash` (line 2205)

- HTTP verbs: `PUT`
- Path segments: `(none detected)`
- JSON fields written: `accountType`, `content`, `dinkAccountHash`, `duplicate`, `embeds`, `extra`, `gameMessage`, `milestone`, `petName`, `playerName`, `previouslyOwned`, `rarity`, `seasonalWorld`, `tts`, `type`, `world`
- Request body expressions: `output.toByteArray())`

## `discordNotificationRequest` (line 2333)

- HTTP verbs: `POST`
- Path segments: `notifications/discord`

## `author` (line 2353)

- HTTP verbs: `PUT`
- Path segments: `(none detected)`

## `configurePolling` (line 2991)

- HTTP verbs: `unknown`
- Path segments: `(none detected)`

## `fetchMessages` (line 3023)

- HTTP verbs: `GET, PUT`
- Path segments: `messages`
- Query parameters: `after`, `sessionStart`
- Headers: `X-Live-On-Cleared-At`, `X-Live-On-Latest-Message-Id`
- Gson response types: `ClanMessage[].class`

## `removePanelNotice` (line 3262)

- HTTP verbs: `DELETE`
- Path segments: `admin/panel-notice`

## `deleteLiveChannel` (line 3456)

- HTTP verbs: `DELETE`
- Path segments: `admin/live-channels`

## `deleteMvpMember` (line 3560)

- HTTP verbs: `DELETE`
- Path segments: `admin/mvp-members`

## `fetchPbRanking` (line 3672)

- HTTP verbs: `GET`
- Path segments: `stats/pb-ranking`
- Query parameters: `boss`, `mode`, `teamSize`, `timeType`
- Gson response types: `PbRankingResponse.class`

## `deleteClanTagPath` (line 3850)

- HTTP verbs: `DELETE, GET`
- Path segments: `(none detected)`

## `publishDraft` (line 4095)

- HTTP verbs: `POST, PUT`
- Path segments: `messages`
- Request body expressions: `jsonBody`

## `deleteSentMessage` (line 4252)

- HTTP verbs: `DELETE`
- Path segments: `admin/messages`

## `togglePinnedMessage` (line 4291)

- HTTP verbs: `POST, PUT`
- Path segments: `admin/messages/pin`
- JSON fields written: `pinned`, `playerName`
- Request body expressions: `gson.toJson(payload)`

## `requestRank` (line 4351)

- HTTP verbs: `POST, PUT`
- Path segments: `messages`
- JSON fields written: `message`, `mode`, `playerName`
- Request body expressions: `gson.toJson(requestPayload)`

## `fetchRankRequestStatus` (line 4425)

- HTTP verbs: `GET`
- Path segments: `rank-request/status`
- Gson response types: `com.google.gson.JsonObject.class`

## `verifyToken` (line 4473)

- HTTP verbs: `GET, PUT`
- Path segments: `v2/players/groups`
- Query parameters: `limit`
- Headers: `Accept`, `User-Agent`

## `postJson` (line 4662)

- HTTP verbs: `POST`
- Path segments: `(none detected)`
- Request body expressions: `json`

## `getJson` (line 4677)

- HTTP verbs: `GET`
- Path segments: `(none detected)`

## `serverBaseUrl` (line 4770)

- HTTP verbs: `unknown`
- Path segments: `(none detected)`

## `resolveRankRequest` (line 4993)

- HTTP verbs: `POST, PUT`
- Path segments: `admin/rank-requests/decision`
- JSON fields written: `decision`, `playerName`
- Request body expressions: `gson.toJson(payload)`

## `deleteRankRequest` (line 5047)

- HTTP verbs: `DELETE`
- Path segments: `(none detected)`

## `confirmRankRequest` (line 5077)

- HTTP verbs: `POST, PUT`
- Path segments: `messages`
- Request body expressions: `gson.toJson(map)`
