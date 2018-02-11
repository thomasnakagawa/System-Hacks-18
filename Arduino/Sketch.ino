// give it a name:
int led1 = 11;
int led2 = 10;
int led3 = 9;

int brightness = 0;    // how bright the LED is
int fadeAmount = 1;    // how many points to fade the LED by

char c;
char prev = 49;

void setup()
{
  Serial.begin(9600);
  pinMode(led1, OUTPUT);
  pinMode(led2, OUTPUT);
  pinMode(led3, OUTPUT);
}

void loop()
{
  digitalWrite(led1, HIGH);   // turn the LED on (HIGH is the voltage level)
 
  if (Serial.available())
  {
    
    c = Serial.read();
    Serial.println("C: ");
    Serial.println(c);
    Serial.println("Prev: ");
    Serial.println(prev);
    
    if (c == 49) {
      if ( prev == 50) {
         analogWrite(led3, 0);
        brightness = 150;
        for (int i = 0; i <= 150; i++) {
          analogWrite(led2, brightness);
          brightness = brightness - fadeAmount;
          delay(30);
        }
      } else {
        brightness = 0;
        analogWrite(led2, brightness);
        analogWrite(led3, brightness);
      }

      // Level 2
    } else if (c == 50) {
      
      if (prev == 49) {
        brightness = 0;
        for (int i = 0; i <= 150; i++) {
          analogWrite(led2, brightness);
          brightness = brightness + fadeAmount;
          delay(30);
        }
      }else {
        Serial.println("Came");
        brightness = 150;
        for (int i = 0; i <= 150; i++) {
          analogWrite(led3, brightness);
          brightness = brightness - fadeAmount;
          delay(30);
        }
      }
      //Level 3
    }else if (c == 51) {

      analogWrite(led1, 255);
      analogWrite(led2, 255);
      brightness = 0;
      for (int i = 0; i <= 150; i++) {
        analogWrite(led3, brightness);
        brightness = brightness + fadeAmount;
        delay(30);
      }
    }

  }
  prev = c;

}
