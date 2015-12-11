include "common/fb303/if/fb303.thrift"

namespace java com.facebook.maestro
namespace java.swift com.facebook.swift.maestro
namespace py maestro.Maestro
namespace py.twisted maestro_twisted.Maestro

//
// data type definitions
//

struct TimeValue {
  1: i64 unixTime,
  2: double value,
}

struct AggSettings {
  1: string name,
  2: i32 interval,
  3: bool cross_datacenter = 0,
  4: i32 category_id = 0,
}

// application represents the high level application name. for system data like cpu, load, uptime
//   metrics, application = "system" for fb303 counters, application = "tiername". If another program
//   was submitting data to ODS, the program name could be the application (ie "newsfeed").
// key is the actual metric. "cpu-user" is an example.
// location is the hostname (with ".facebook.com" stripped off). webrs001.sctm is an example.
// internally, ODS combines the application and key to form a pseudo key called "application.key",
//   and this should be used when searching for keys.
// entities are normally machine names, or rolled up tier names. webrs001.sctm and mc.sf2p are
//   examples.

struct ODSValue {
  1: i64 unixTime,
  2: double value,
  3: string application,
  4: string key,
  5: string location,
}

struct OdsAggValue {
  1: string entity,
  2: string key,
  3: i64 unixTime,
  4: double avg,
  5: double sum,
  6: double min,
  7: double max,
  8: i32 count,
  9: i32 category_id = 0,
}

/**
 * pre-aggregated data point with tags sent to super aggregator
 * for global aggregation
 */
struct ODSAggValueWithTags {
  1: string entity,
  2: string key,
  3: i64 unixTime,
  // Stores average. Using field name value so that it is compatible with
  // template class defined in aggregator. This type is only used by
  // aggregator to pass data points between super aggregators.
  4: double value,
  5: double sum,
  6: double min,
  7: double max,
  8: i32 count,
  // By default data is treated as being sent every 4 minutes
  9: i32 interval = 240,
  10: list<AggSettings> tags,
  11: i32 category_id = 0,
}

enum OdsInterval {
  ODS_ROLLUP_INTERVAL = 240,
}

enum OdsKeyType {
  ODS_REGULAR_KEY = 0,
  ODS_EXCLUDED_KEY = 1,
  ODS_INCLUDED_KEY = 2,
  ODS_NEW_KEY = 3,
}

enum OdsDataType {
  ODS_DATA_DOUBLE = 0,
  ODS_DATA_STRING = 1,
}

struct OdsKeyEntityMapperResult {
  1: i32 total_results,
  2: list<string> objects,
}

/**
 * an enum that can be used for sql ordering
 */
enum OdsEntityOrderByType {
  ODS_ENTITY_BY_NAME = 0,
  ODS_ENTITY_NONE = 1, // no ordering done on sql statement
}

/**
 * some entity types that are used in
 * the ods search utils page. in that php page,
 * they will be migrated to use this.
 */
enum OdsEntityGroupTypes {
  ENTITY_TYPE_APPLICATION = 0,
  ENTITY_TYPE_OTHERS = 0,
  ENTITY_TYPE_HOST_NAME = 1,
  ENTITY_TYPE_AGGREGATED = 2,
  ENTITY_TYPE_SMC_TIER = 2,
  ENTITY_TYPE_FBNET = 3,
  ENTITY_TYPE_NETWORK_LATENCY = 4,
  ENTITY_TYPE_STORAGE = 5,
  ENTITY_TYPE_NETWORK = 6,
  ENTITY_TYPE_DATACENTER_MAINTENANCE_STATUS = 7,
  ENTITY_TYPE_POWER = 8,
  ENTITY_TYPE_DEAD = 9, // dead entities and keys have same type.

  KEY_TYPE_DEAD = 9,
  APP_GROUP_THRESHOLD = 5,
}


enum OdsAggregationType {
  ODS_AGGREGATION_TYPE_DEFAULT  = 1,
  ODS_AGGREGATION_TYPE_AVG      = 2,
  ODS_AGGREGATION_TYPE_SUM      = 4,
  ODS_AGGREGATION_TYPE_COUNT    = 8,
  ODS_AGGREGATION_TYPE_MAX      = 16,
  ODS_AGGREGATION_TYPE_MIN      = 32,
}

struct ODSData {
  1: i64            unixTime,
  2: string         value,
  3: string         entity,
  4: string         key,
  5: OdsDataType    dataType,
}

struct ODSFbagentData {
  1: i64            unixTime,
  2: string         value,
  3: string         entity,
  4: string         key,
  5: OdsDataType    dataType,
  6: i32            keyType,
  // By default data is treated as being sent every 4 minutes
  7: i32            interval = 240,
  8: i32            category_id = 0,
}

struct ODSRateOfChangeKeyInfo {
  1: string key,
  2: i64 unixTime,
  3: i64 allowable_time_diff,
  4: i64 value,
  5: i64 min_rate,
  6: i64 max_rate,
}

struct ODSRateOfChangeValue {
  1: string entity,
  2: list<ODSRateOfChangeKeyInfo> keyinfo,
}

struct ODSHistoricalPurgeDefn {
  1: string entity,
  2: string key,
  3: i64 timeStart,
  4: i64 timeEnd,
}

struct ODSAppValue {
  1: i64 unixTime,
  2: double value,
  3: string entity,
  4: string key,
  // By default data is treated as being sent every 4 minutes
  5: i32 interval = 240,
  6: i32 category_id = 0,
}


struct ODSAppValueWithTags {
  1: i64                        unixTime,
  2: double                     value,
  3: string                     entity,
  4: string                     key,
  5: i32                        interval = 240,
  6: list<AggSettings>          tags,
  7: i32                        category_id = 0,
}

struct ODSAppValueWithTagsList {
  1: list<ODSAppValueWithTags> datapoints;
}

struct ODSFbagentDataWithTags {
  1: i64                        unixTime;
  2: string                     value;
  3: string                     entity;
  4: string                     key;
  5: OdsDataType                dataType;
  6: i32                        keyType;
  7: i32                        interval = 240;
  8: list<AggSettings>          tags;
  9: i32                        category_id = 0;
}

struct ODSFbagentDataWithTagsList {
  1: list<ODSFbagentDataWithTags> datapoints;
}

struct ODSAppValueWithAggType {
  1: i64 unixTime;
  2: double value;
  3: string entity;
  4: string key;
  5: OdsAggregationType aggType;
  6: i32 category_id = 0;
}

struct ODSSnapshotValue {
  1: string entity;
  2: string value;

  // there is no data for this entity, but the entity is a valid host
  3: bool isValid;

  // the host is not alive (there is no data in ODS for this entity)
  4: bool isAlive;
}

struct ODSSnapshotFilter {
  1: string inRgexString;
  2: string notInRgexString;
  3: string valueString;
}

// Abacus can submit data to ods, and uses its own structure.
// entity represents the application name (for rolled up data) or tier name (for rolled up data).
//   Examples are "search" for an application or "src.sctm" for a rolled up tier.
//   Abacus does not submit host level data.
// key represents the already combined "application.key"
struct AbacusValue {
  1: string entity;
  2: string key;
  3: i64 unixTime;
  4: double value;
}

struct AggEntityValue {
  1: map<string, list<TimeValue> > values;
  2: set<string> entities;
}

struct TransformResult {
  1: map<string, map<string, string>> values;
  2: set<string> entities;
}

enum OdsExceptionCode {
  ODS_EXCEPTION_DEFAULT = 0,
  ODS_EXCEPTION_DBCONN_ERROR = 1,
  ODS_EXCEPTION_SQL_ERROR = 2,
  ODS_EXCEPTION_DB_NOTFOUND = 3,
  ODS_EXCEPTION_SMC_CONN_ERROR = 4,
  ODS_EXCEPTION_SMC_DATA_ERROR = 5,
  ODS_EXCEPTION_DEPRECIATED_METHOD = 6,
  ODS_EXCEPTION_ENTITY_NOT_FOUND = 7,
  ODS_EXCEPTION_SERVICE_CONN_ERROR = 8,
  ODS_EXCEPTION_KEY_NOT_FOUND = 9,
}

enum OdsDataTable {
  ODS_TABLE_AUTO = 0,
  ODS_TABLE_RAW = 1,
  ODS_TABLE_WEEK = 2,
  ODS_TABLE_MONTH = 3,
  ODS_TABLE_YEAR = 4,
  ODS_TABLE_CAPACITY = 5,
}

typedef i32 OdsAdHocAggType

enum OdsAdHocAggTypeBitmapValues {
  ODS_ADHOC_AGG_NONE = 0,
  ODS_ADHOC_AGG_SUM  = 1,
  ODS_ADHOC_AGG_AVG  = 2,
  ODS_ADHOC_AGG_MAX  = 4;
  ODS_ADHOC_AGG_MIN  = 8;
  ODS_ADHOC_AGG_MEDIAN  = 16;
} (bitmask = 1)

enum OdsTopCriteria {
  ODS_TOP_N_MAX                 = 1,
  ODS_TOP_N_MIN                 = 2,
  ODS_TOP_N_95_PERCENTILE_MAX   = 3,
  ODS_TOP_N_95_PERCENTILE_MIN   = 4,
  ODS_TOP_N_AVG_MAX             = 5,
  ODS_TOP_N_AVG_MIN             = 6,
}

/**
 * @param n           calculate top n if this is >= 0
 * @param criteria    how to calculate top N
 * @param aggType     whether to aggregate the rest of the bucket.
 *                    Use ODS_ADHOC_AGG_NONE to turn this off; otherwise,
 *                    specify the type of aggregation.
 * @param useEntityAsSmcTier    When TopN is turned on and this is true,
 *                    entities are treated as SMC tier name, and
 *                    query will be done on its services and its descendants'
 *                    services.
 * @param useEntityAsHostPort   When TopN is turned on and this is true,
 *                    host:port is used instead of just host
 */
struct ODSTopN {
  1: i32             n,
  2: OdsTopCriteria  criteria,
  3: OdsAdHocAggType aggType,
  4: bool useEntityAsSmcTier,
  5: bool useEntityAsHostPort,
}

struct RecentEntitiesOrKeys {
  1: i32 highest_id,
  2: list<string> names,
}

struct KeyStat {
  1: string key,
  2: double count,
}

/**
 * Wrapper around a map of a map that our results are commonly expressed as:
 * map<string,      map<string, list<TimeValue>>>
 *       |               |         |
 *       V               V         V
 * map<entity_name, map<key,    list of unixTimes and values>
 * Introduced for easier thrift serialization and better cross-language
 * support and use, especially for verbose languages like Java.
 */
struct OdsGetResult {
  1: map<string, map<string, list<TimeValue> > > result,
}

/**
 * This is parameter structure for getTimeSeries.
 */
struct GetTimeSeriesParams {
  1: list<string> entities,
  2: list<string> keys,
  3: i64 startTime,
  4: i64 endTime,
  5: OdsDataTable table,
  6: i32 window,
  7: OdsAggregationType aggregationType,
  8: string traceData,
}

/**
 * This is a results holder structure for getTimeSeries.
 */
struct GetTimeSeriesResults {
  1: map<string, map<string, list<TimeValue>>> data,
}

/**
 * This is parameter structure for getTimeSeriesByAggType.
 *
 * @param entities                list of entity names
 * @param keys                    list of key names
 * @param start                   start unix time in seconds
 * @param end                     end unix time in seconds
 * @param table                   table to query
 * @param window                  the bucket window for adhoc aggregation
 * @param aggregationTypeBitMap   roll up aggregation type (bitmap)
 * @param traceData               data used for tracing, "" means no tracing
 */
struct GetTimeSeriesByAggTypeParams {
  1: list<string>entities,
  2: list<string>keys,
  3: i64 startTime,
  4: i64 endTime,
  5: OdsDataTable table,
  6: i32 window,
  7: i32 aggregationTypeBitMap,
  8: string traceData,
}

/**
 * This is a results holder structure for getTimeSeriesByAggType.
 *
 * @return a map of OdsAggregationType to OdsGetResults
 */
struct GetTimeSeriesByAggTypeResults {
  1: map<OdsAggregationType, OdsGetResult> data,
}

exception OdsException {
  1: string message;
  2: OdsExceptionCode code;
}

//
// service definitions
//
service Maestro extends fb303.FacebookService
{
  // Assumes the key has already been combined to form an entry like "application.key"
  // from the calling application.
  // entity should have ".facebook.com" stripped off.
  void setOdsValues(1: list<ODSAppValue> data)
    throws(-1:OdsException oe);

  /**
   * For ODS router called by aggregation Service used only,
   * to set aggregated values back to ODS aggregation table
   * @param data            list of aggregated values
   */
  void setOdsAggValues(1: list<OdsAggValue> data)
    throws (1: OdsException oe);

  void setOdsRollupValues(1: list<ODSAppValue> data)
    throws(-1:OdsException oe);

  void setOdsRateOfChangeValues(1: list<ODSRateOfChangeValue> data)
    throws (-1:OdsException oe);

  void setOdsValuesWithHostRollup(
    1: list<ODSValue> data,
    2: bool doHostRollup)
    throws (-1:OdsException oe);

  list<TimeValue> getOdsValues(
    1: string entity,
    2: string key,
    3: i64 start,
    4: i64 end,
    5: OdsDataTable table,
    6: i32 aggregationType)
    throws (-1:OdsException oe);

  // writes historical data to ods.
  // note if there is existing data already in ods, then it will fail. if this
  // is the case, you need to call the deleteOdsHistoricalData method to
  // purge the data first. becareful if you do this, because it can't be undone.
  // also note, this api is intended to be used for doing historical data
  // imports. the idea is that it won't be high traffic. if that is your case,
  // then it will break things, so please don't do that.
  void setOdsHistoricalData(-1: list<ODSAppValue> data)
    throws (-2:OdsException oe);

  // this purges data specified. becareful if you do this. it will
  // permanently remove things from ods with no way to get it back.
  // if you mess up other people's things, many people will get mad.
  // this is intended to have low traffic as well.
  void deleteOdsHistoricalData(-1: list<ODSHistoricalPurgeDefn> data)
    throws(-2:OdsException oe);


  // Retrieve a snapshot data for the data queried
  // 1: list of entities, which could be list of regular expressions
  // 2: list of keys
  // 3: the time get the snapshot from; pass current time to get the latest
  //    snapshot
  // 4: a filter on entities to limit the number of results
  // 5: if true, entities that did not found any value based on
  //    the query will be returned with isValid and isAlive
  //    flag set.  Otherwise only entities with data is returned.
  // 6: if non-zero, this overrides the time period for "acceptable"
  //    data (regardless of the roll up table queried).  If zero,
  //    this method automatically defaults the time period to find
  //    the snapshot data based on the data type and the timestamp given.
  //    Unit in seconds.
  map<string, list<ODSSnapshotValue>> getSnapshotWithFilters(
    1: list<string> entities,
    2: list<string> keys,
    3: i64  timeStamp,
    4: ODSSnapshotFilter filter,
    5: bool findMissingValue,
    6: i32  periodOverride,
    7: bool entityIsRegex,
    8: bool keyIsRegex);

  /**
   * Retrieve snapshot data, return given result in a compressed zlib
   * string format.
   * @param entities    list of entities to query
   * @param keys        list of keys to query
   * @param timestamp   time to look for data; data will be search from this
   *                    time to the past for values.  Use current time to view
   *                    the current snapshot.
   * @param filter      For filtering data by regex in entities or by value
   *                    criteria
   * @param findMissingValue
   *                    If true, the query look for any entities matched
   *                    but did not retrieve a value, then treat them as host
   *                    name and query system.cpu-idle to see if they are
   *                    alive.  If they are, the alive flag will be set.
   *                    (see result for details)
   * @param periodOverride
   *                    A snapshot value is retrieving the latest value
   *                    given from the specified time to some default range
   *                    of value depending on the table we need to query
   *                    If the data you queried as submitting data less
   *                    frquent than per 4 min, you might need to change
   *                    this override time to query for longer period.
   *                    Please note that the longer you query, the slower
   *                    the query will be, so use only what you need.
   * @param entitYIsRegex
   *                    true if the entities specified is in regex
   * @param keyIsRegex  true if the keys specified is in regex
   *
   * @return  a map of key name to compressed strings.  The string values
   * are compressed by Zlib and when uncompressed, are in the following
   * format:
   * <entity>|<value>|<valid>|<alive>#@#<entity>|<value>|<valid><alive>...
   * where
   * entity is the entity name, value is the numerical or string values,
   * valid is true if data is retrieved for this entity when findMissingValue
   * is turned on, and alive is true if data is missing but the entity has
   * data for system.cpu-idle at that time period.
   */
  map<string, string> getSnapshotCompressed(
    1: list<string> entities,
    2: list<string> keys,
    3: i64  timeStamp,
    4: ODSSnapshotFilter filter,
    5: bool findMissingValue,
    6: i32  periodOverride,
    7: bool entityIsRegex,
    8: bool keyIsRegex);

  map<string, map<string, list<TimeValue> > > getEntityValues(
    1: list<string> entities,
    2: list<string> keys,
    3: i64 start,
    4: i64 end,
    5: OdsDataTable table,
    6: i32 window,
    7: i32 aggregationType)
    throws(-1:OdsException oe);

  GetTimeSeriesResults getTimeSeries(
    1: GetTimeSeriesParams query)
    throws(-1:OdsException oe);

  map<string, map<string, string> > getEntityValuesCompressed(
    1: list<string> entities,
    2: list<string> keys,
    3: i64 start,
    4: i64 end,
    5: OdsDataTable table,
    6: i32 window,
    7: i32 aggregationType)
    throws(1:OdsException oe);

  map<string, map<string, list<TimeValue> > > getRegexEntityValues(
    1: list<string> entities,
    2: list<string> keys,
    3: i64 start,
    4: i64 end,
    5: OdsDataTable table,
    6: i32 window,
    7: i32 aggregationType)
    throws(-1:OdsException oe);

  map<string, map<string, string> > getRegexEntityValuesCompressed(
    1: list<string> entities,
    2: list<string> keys,
    3: i64 start,
    4: i64 end,
    5: OdsDataTable table,
    6: i32 window,
    7: i32 aggregationType)
    throws(1:OdsException oe);

  AggEntityValue getEntityAggValues(
    1: list<string> entities,
    2: list<string> keys,
    3: i64 start,
    4: i64 end,
    5: OdsDataTable table,
    6: OdsAdHocAggType type,
    7: i32 window,
    8: bool bypassLimit,
    9: i32 aggregationType)
    throws (-1:OdsException oe);

  AggEntityValue getRegexEntityAggValues(
    1: list<string> entities,
    2: list<string> keys,
    3: i64 start,
    4: i64 end,
    5: OdsDataTable table,
    6: OdsAdHocAggType type,
    7: i32 window,
    8: bool bypassLimit,
    9: i32 aggregationType)
    throws (-1:OdsException oe);

  string getAggSources(
    1: bool rgex,
    2: list<string> entities,
    3: list<string>keys,
    4: i64 start,
    5: i64 end,
    6: i32 window);

  /**
   * API that allows much tweaks in calculating and retrieving ODS
   * time series.  ODS chart page uses this.
   *
   * @param regex      true if the entities is specified in regular expression
   * @param entities   list of entity names
   * @param keys       list of key names
   * @param start      start unix time in seconds
   * @param end        end unix time in seconds
   * @param table      table to query
   * @param type       adhoc aggregation type (bitmap)
   * @param window     the bucket window for adhoc aggregation
   * @param transformConfig       a json config string to tell the types of transformation needed
   * @param aggregationTypeBitMap roll up aggregation type (bitmap)
   * @param bestFitDegree         best fit degree.  If 0, no best fit curve is calculated.  If > 0,
   * this will be the mth degree for calculating best-fit curve by
   * least squares.
   * @param transformDataSources   if specified, this is the list of data sources
   * @param topNOpts   top n option
   *
   * @return a map of OdsAggregationType to TransformResult. TransformResult is a complex
   * data type as follow:
   * struct TransformResult {
   *   1: map<string, map<string, string>> values;
   *   2: set<string> entities;
   * }
   *
   * Note that the member values is again a map of map. The meanings are shown below:
   *
   * map<string,      map<string, string>>
   *       |               |         |
   *       V               V         V
   * map<entity_name, map<key, data_json>
   */
  map<OdsAggregationType, TransformResult>
  getOdsTimeSeriesByAggregationType(
    1: bool regex,
    2: list<string>entities,
    3: list<string>keys,
    4: i64 start,
    5: i64 end,
    6: OdsDataTable table,
    8: OdsAdHocAggType type,
    9: i32 window,
    10: string transformConfig,
    11: i32 aggregationTypeBitMap,
    12: i32 bestFitDegree,
    13: string transformDataSources = "",
    14: ODSTopN topNOpts);

  /**
   * API that allows much tweaks in calculating and retrieving ODS
   * time series in a zlib string format.  ODS chart page uses this.
   *
   * @param regex      true if the entities is specified in regular expression
   * @param entities   list of entity names
   * @param keys       list of key names
   * @param start      start unix time in seconds
   * @param end        end unix time in seconds
   * @param table      table to query
   * @param type       adhoc aggregation type (bitmap)
   * @param window     the bucket window for adhoc aggregation
   * @param transformConfig       a json config string to tell the types of transformation needed
   * @param aggregationTypeBitMap roll up aggregation type (bitmap)
   * @param bestFitDegree         best fit degree.  If 0, no best fit curve is calculated.  If > 0,
   * this will be the mth degree for calculating best-fit curve by
   * least squares.
   * @param transformDataSources   if specified, this is the list of data sources
   * @param topNOpts   top n option
   *
   * @return a map of OdsAggregationType to TransformResult. TransformResult is a complex
   * data type as follow:
   * struct TransformResult {
   *   1: map<string, map<string, string>> values;
   *   2: set<string> entities;
   * }
   *
   * Note that the member values is again a map of map. The meanings are shown below:
   *
   * map<string,      map<string, string>>
   *       |               |         |
   *       V               V         V
   * map<entity_name, map<key, data_json>
   *
   * This function will compress data_json string with zlib.
   */
  map<OdsAggregationType, TransformResult>
  getOdsTimeSeriesByAggregationTypeCompressed(
    1: bool regex,
    2: list<string>entities,
    3: list<string>keys,
    4: i64 start,
    5: i64 end,
    6: OdsDataTable table,
    8: OdsAdHocAggType type,
    9: i32 window,
    10: string transformConfig,
    11: i32 aggregationTypeBitMap,
    12: i32 bestFitDegree,
    13: string transformDataSources = "",
    14: ODSTopN topNOpts);

  /**
   * API that allows gets timeseries per aggregation type using a bitmap
   * of aggregation types. See OdsAggregationType for the values that
   * can be used to build up the bitmap.
   *
   * @param entities                list of entity names
   * @param keys                    list of key names
   * @param start                   start unix time in seconds
   * @param end                     end unix time in seconds
   * @param table                   table to query
   * @param window                  the bucket window for adhoc aggregation
   * @param aggregationTypeBitMap   roll up aggregation type (bitmap)
   *
   * @return a map of OdsAggregationType to OdsGetResults
   */
  map<OdsAggregationType, OdsGetResult>
  getOdsTimeSeriesSimple(
    1: list<string>entities,
    2: list<string>keys,
    3: i64 start,
    4: i64 end,
    5: OdsDataTable table,
    6: i32 window,
    7: i32 aggregationTypeBitMap);

  /**
   * API that allows gets timeseries per aggregation type using a bitmap
   * of aggregation types. See OdsAggregationType for the values that
   * can be used to build up the bitmap.
   * This version allows tracing.
   *
   * @param query                   structure that holds all parameters
   *
   * @return results holder structure
   */
  GetTimeSeriesByAggTypeResults
  getTimeSeriesByAggType(
    1: GetTimeSeriesByAggTypeParams query);


  /**
   * Similar to above API, except the data per aggregation type is in
   * the form of zlib compressed, thrift serialized strings of OdsGetResults.
   * The charts and dashboards will use this API.
   *
   * @param entities                list of entity names
   * @param keys                    list of key names
   * @param start                   start unix time in seconds
   * @param end                     end unix time in seconds
   * @param table                   table to query
   * @param window                  the bucket window for adhoc aggregation
   * @param aggregationTypeBitMap   roll up aggregation type (bitmap)
   *
   * @return a map of OdsAggregationType to zlib compressed string
   *  representing a serialized, compressed OdsGetResult
   */
  map<OdsAggregationType, string>
  getOdsTimeSeriesSimpleCompressed(
    1: list<string>entities,
    2: list<string>keys,
    3: i64 start,
    4: i64 end,
    5: OdsDataTable table,
    6: i32 window,
    7: i32 aggregationTypeBitMap);

  // Gives the most flexibility in defining a single transform config tree
  // that will result in a time series data, but is probably the hardest
  // to use.  The two parameters are json strings:
  //
  // 1: start time (unix time) for data
  // 2: end time for data
  // 3: dataJSON    ex. {"0:0":{"entities":["webrs009.ash1"],
  //                       "keys":["system.cpu-idle"], "table":"month"},
  //                     "0:1":{"entities":["webrs008.ash1"],
  //                       "keys":["system.cpu-user"]}}
  //
  // The available attributes for dataJSON are:
  //  - entities,
  //  - keys,
  //  - table ("raw", "week", "month", "year", "capacity", default: "auto"),
  //  - aggregation_type ("avg", "sum", "count", "max", "min") - if this is
  //      an aggregate entity, then you must choose which type of aggregate
  //      data you want. If you don't specify anything, then 'default' is chosen
  //      which queries the regular ods ddbs instead of the aggregate ddbs
  //  - window (bucket/agg window in minutes, default: 240),
  //  - adhoc_agg_type ("none", "sum", "avg")
  //  - regex (boolean)
  //
  //
  // This determines the source of the transformation with, the first map
  // indexed by an unique identifer.  (For the ODS transform page, that
  // identifier indicate the tree-depth of the transformation
  // tree as shown in the example above).  This index matches the
  // configJSON data argument for the
  // transform library to know which sources to transform.
  //
  // 4: configJSON
  // ex. [{"type":"formula","config":{"expr":"(+ $1 $2)"},
  //       "data":[
  //         {"type":"rate","config":{"duration":1,"window_size":2,
  //          "time_window_size":"","ignore_negative":false,"percent":false},
  //          "data":"0:0"},
  //         {"type":"mov_avg","config":{"window_size":5,"time_window_size":""},
  //          "data":"0:1"}]}]
  // The json is a list of transformation trees or chains.  A transformation
  // configuration can be a tree when the transformation takes more than
  // one data source (ex. formula).  The json format is generally of the
  // format:  { "type":<type>, "config":{ <configuration parameters },
  //            "data":<either another transformation, or a string to indicate
  //                    the data source to use> }
  //
  // Return a list of time series in json format.
  //
  // For all the valid transform type and parameters, see
  //   fbomb/sherlock/lib/cpp/DetectorConfig.json
  list<string> getAdvancedTransformQuery(
    1: i64 start_date,
    2: i64 end_date,
    3: string dataJSON,
    4: string configJSON)
    throws (-1:OdsException oe);

  list<KeyStat> getTopNActiveKeyPrefix(1: i32 n);

  // api that fbagent uses. rolls up data per tier
  // (webrs.sctm for example) and also adds host level data.
  void setMultipleOdsValues(1: list<ODSValue> data)
    throws (-1:OdsException oe);

  // a new methods for sending generic data to ODS
  void setODSData (1: list<ODSData> data)
    throws (-1:OdsException oe);

  void setOdsFbagentHostData (
    1: list<ODSFbagentData> data)
    throws (-1:OdsException oe);

  void setOdsFbagentData (
    1: list<ODSFbagentData> data,
    2: bool skiphostdata)
    throws (-1:OdsException oe);

  list<string> getEntityNames();

  list<string> getKeyNames();

  map<string, list<string> > getKeyMapping(
    1: list<string> entities,
    2: i64 minutes_back);

  map<string, list<string> > getEntityMapping(
    1: list<string> keys,
    2: i64 minutes_back);

  // Returns a list of entities that are 'aggregate entities'.
  // Aggregate entities are those for which we do multiple aggregations like
  // sum, count, avg, min, max.
  // You can optionally specify a regular expression and only aggregate entities
  // whose names match the regular expression will be returned.
  set<string> getAggregateEntityNames(1: string regexp);

  map<string, list<string> > getMapping(
    1: list<string> entities,
    2: i64 minutes_back,
    3: bool get_key)
    throws (-1:OdsException oe);

  /**
   * for a given entity, find corresponding keys
   */
  OdsKeyEntityMapperResult getCorrespondingKeys(
    1: list<string> keywords,
    2: list<string> entities,
    3: i32 minutes_back,
    4: i32 limit)
    throws (-1:OdsException oe);

  /**
   * for a given key, find corresponding entities.
   */
  OdsKeyEntityMapperResult getCorrespondingEntities(
    1: list<string> keywords,
    2: list<string> keys,
    3: i32 minutes_back,
    4: i32 limit)
    throws (-1:OdsException oe);

  /**
   * For an entity type, and an optional list of regular expressions,
   * find the entities. if the regexes are empty, then none will be used.
   * an OR operation is performed on the regexes.
   *
   * @param entity_type   Entity type ID, see OdsEntityGroupTypes for the value.
   *                      Or
   *                      -1 for everything using cache,
   *                      -2 for everything using DB data.
   *
   */
  OdsKeyEntityMapperResult getEntitiesByTypeAndRegexs(
    1: i32 entity_type,
    -1: i32 limit,
    3: OdsEntityOrderByType ordering,
    4: list<string> regexes)
    throws (-2:OdsException oe);

  /**
   * for a set of keywords, find the keys by regexes. it does an OR
   * operation for the regexes.
   * if no keywords are specified, nothing is returned.
   */
  OdsKeyEntityMapperResult getKeysByRegexs(
    1: list<string> keywords,
    2: i32 limit,
    3: bool use_cdbdao)
    throws(-1:OdsException oe);

  /**
   * get all alive prefixes for numeric keys
   */
  list<string> getAllAliveNumericKeyPrefixes(
    1: bool returnKeysWithNoDelimiter,
    2: i32 limit)
    throws (-1:OdsException oe);

  /**
   * get numeric keys that are alive with the passed in prefix.
   */
  list<string> getAllAliveNumericKeys(
    1: string prefix,
    2: i32 limit)
    throws (-1:OdsException oe);

  /**
   * get all keys that match the prefix and type
   */
  list<string> getAllKeys(
    1: string prefix,
    2: i32 key_type);

  /**
   * get global aggregation entities that match "%.global"
   */
  list<string> getGlobalAggregationEntities();

  /**
   * for a given keyword, says if there is an exact match.
   * it works for both keys an entities.
   */
  bool findExactMatch(
    1: string keyword_to_search,
    2: bool is_entity);

  /**
   * search for key or entity by doing an AND operation
   */
  OdsKeyEntityMapperResult searchEntityOrKey(
    1: list<string> keywords,
    2: i32 limit,
    3: bool is_entity)
    throws (-1:OdsException oe);

  /**
   * given an entity_id, return all the name of the entities that
   * have higher id number than the given entity_id.
   */
  RecentEntitiesOrKeys getRecentEntities(
    1: i32 entity_id);

  /**
   * given an key_id, return all the name of the keys that
   * have higher id number than the given key_id.
   */
  RecentEntitiesOrKeys getRecentKeys(
    1: i32 key_id);

  /**
   * given an entity name, return its id. Goes through the cache layer.
   */
  i64 getEntityIdByName(
    1: string name)
    throws (1:OdsException oe);

  /**
   * get the max entity id in CdbDAO.
   */
  i64 getMaxEntityId();

  /**
   * given a key name, return key id. Goes through the cache layer.
   */
  i64 getKeyIdByName(
    1: string name)
    throws (1:OdsException oe);

  /**
   * get the max entity id in CdbDAO.
   */
  i64 getMaxKeyId();

  /**
   * given an entity id, return its DDB. Goes through the cache layer.
   */
  i64 getDdbIdByEntityId(
    1: i64 entityId)
    throws (1:OdsException oe);

  /**
   * given an entity_id, return the aggregate DDB id. Goes through the cache
   * layer.
   */
  i64 getAggregateDdbIdByEntityId(
    1: i64 entityId)
    throws (1:OdsException oe);
}
