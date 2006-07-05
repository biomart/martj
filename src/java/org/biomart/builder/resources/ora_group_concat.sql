-- Code copied from http://www.orafaq.com/forum/t/57395/0/

CREATE OR REPLACE TYPE concat_expr AS OBJECT (
  str VARCHAR2 (4000),
  del VARCHAR2 (4000));
/

CREATE OR REPLACE TYPE group_concat_ot AS OBJECT (
  str VARCHAR2 (4000),
  del VARCHAR2 (4000),

  STATIC FUNCTION odciaggregateinitialize (
	 sctx IN OUT group_concat_ot)
	 RETURN NUMBER,

  MEMBER FUNCTION odciaggregateiterate (
	 SELF IN OUT group_concat_ot,
	 ctx IN concat_expr)
	 RETURN NUMBER,

  MEMBER FUNCTION odciaggregateterminate (
	 SELF IN group_concat_ot,
	 returnvalue OUT VARCHAR2,
	 flags IN NUMBER)
	 RETURN NUMBER,

  MEMBER FUNCTION odciaggregatemerge (
	 SELF IN OUT group_concat_ot,
	 ctx2 group_concat_ot)
	 RETURN NUMBER);
/

CREATE OR REPLACE TYPE BODY group_concat_ot
AS
  STATIC FUNCTION odciaggregateinitialize (
	 sctx IN OUT group_concat_ot)
	 RETURN NUMBER
  IS
  BEGIN
	 sctx := group_concat_ot (NULL, NULL);
	 RETURN odciconst.success;
  END;
  MEMBER FUNCTION odciaggregateiterate (
	 SELF IN OUT group_concat_ot,
	 ctx IN concat_expr)
	 RETURN NUMBER
  IS
  BEGIN
	 IF SELF.str IS NOT NULL THEN
	   SELF.str := SELF.str || ctx.del;
	 END IF;
	 SELF.str := SELF.str || ctx.str;
	 RETURN odciconst.success;
  END;

  MEMBER FUNCTION odciaggregateterminate (
	 SELF IN group_concat_ot,
	 returnvalue OUT VARCHAR2,
	 flags IN NUMBER)
	 RETURN NUMBER
  IS
  BEGIN
	 returnvalue := SELF.str;
	 RETURN odciconst.success;
  END;

  MEMBER FUNCTION odciaggregatemerge (
	 SELF IN OUT group_concat_ot,
	 ctx2 IN group_concat_ot)
	 RETURN NUMBER
  IS
  BEGIN
	 IF SELF.str IS NOT NULL THEN
	   SELF.str := SELF.str || SELF.del;
	 END IF;
	 SELF.str := SELF.str || ctx2.str;
	 RETURN odciconst.success;
  END;
END;
/

CREATE OR REPLACE FUNCTION group_concat (
  ctx IN concat_expr)
  RETURN VARCHAR2 DETERMINISTIC PARALLEL_ENABLE
  AGGREGATE USING group_concat_ot;
/
