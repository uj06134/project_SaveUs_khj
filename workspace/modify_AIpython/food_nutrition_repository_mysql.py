from typing import Optional
import mysql.connector

def get_food_nutrition_by_name(food_name: str) -> Optional[dict]:
    try:
        conn = mysql.connector.connect(
            host="3.37.90.119",
            user="root",
            password="3306",
            database="saveus",
            charset="utf8"
        )
        cursor = conn.cursor()

        query = """
        SELECT * FROM FOOD_NUTRITION
        WHERE FOOD_NAME = %s
        """

        cursor.execute(query, (food_name,))
        row = cursor.fetchone()

        if not row:
            return None

        columns = [desc[0].lower() for desc in cursor.description]
        return dict(zip(columns, row))

    except Exception as e:
        print("MySQL ERROR:", e)
        return None
    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()


if __name__ == "__main__":
    print(get_food_nutrition_by_name("고구마"))
    print(get_food_nutrition_by_name("김밥"))
    print(get_food_nutrition_by_name(""))
