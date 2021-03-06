//#include "opencv2/core/utility.hpp"
#include "opencv2/imgproc.hpp"
#include "opencv2/imgcodecs.hpp"
#include "opencv2/highgui.hpp"
#include "opencv2/core.hpp"
#include <opencv2/objdetect.hpp>
#include <opencv2/video.hpp>
//#include "opencv2/bgsegm.hpp"

#include <iostream>
#include  <vector>

/* Testing functions*/
#include <stdarg.h> //needed for va_args
#include <time.h> //needed for strftime
#include <fstream>
void print(const char* format, ... );
void show(const char *name, cv::Mat &img);
void show(const char *name, cv::Mat &img1, cv::Mat &img2);
cv::Mat merge(cv::Mat &img1, cv::Mat &img2);
/* end testing*/

using namespace std;
using namespace cv;


CascadeClassifier sign_cascade;
string sign_cascade_folder = "../../SignCoachApp/AppAttempt/app/src/main/assets/data/";
string sign_cascade_ext = ".xml";
string hand_type = "R"; 

Mat fgMask; //fg mask fg mask generated by MOG2 method
Ptr<BackgroundSubtractor> pMOG2; //MOG2 Background subtractor

bool checkIfCorrect(Mat &src, char letter);
void fixRotation(Mat &src, Mat &dst, int rotation);
void rot90(Mat &src, int flag);


int main( int argc, char** argv )
{        
    VideoCapture cap(0);

    /* 
    if(argc < 1) {
        return -1;
    }
    VideoCapture cap(argv[1]);
    */

    if(!cap.isOpened())  { // check if we succeeded
        cout << "Failed to open webcam" << endl;
        return -1;
    }

    Mat src; 
    bool loop = true;
    char letter = 'A'; 
    
    //pMOG2 = bgsegm::createBackgroundSubtractorMOG(); 
    pMOG2 = createBackgroundSubtractorMOG2(); 
    
    string sign_cascade_fullpath = sign_cascade_folder + string(1, letter) +  sign_cascade_ext;
    if( !sign_cascade.load( sign_cascade_fullpath ) ){ print("(!) Error loading %s", sign_cascade_fullpath); return false; };
    
    
    while(loop)
    {
        Mat frame;
        cap >> src; // get a new frame from camera
        if (src.empty())
            break;

        Mat dst;
        fixRotation(src, dst, 1);

        Mat found = dst.clone();

        bool result = checkIfCorrect(found, letter);
        string resultStr = "False";
        if (result) resultStr = "True";
        
        //putText(found, "Found " + string(1, letter) + " = " + resultStr, Point2f(20,50), FONT_HERSHEY_PLAIN, 3,  Scalar::all(0), 5);
        //show("Result", found);
        
        cout <<  "Found " + string(1, letter) + " = " + resultStr << endl;
        int key = 0xff & waitKey(250); 
     
        if (key == 27) break;  
        if (key != 255) {
            letter = char(toupper(key)); 
            string sign_cascade_fullpath = sign_cascade_folder + string(1, letter) +  sign_cascade_ext;
            if( !sign_cascade.load( sign_cascade_fullpath ) ){ print("(!) Error loading %s", sign_cascade_fullpath); return false; };
        }

   }


  
   return 0;
}

bool checkIfCorrect(Mat &src, char letter) {
	
    //string sign_cascade_fullpath = sign_cascade_folder + string(1, letter) +  sign_cascade_name;
    //if( !sign_cascade.load( sign_cascade_fullpath ) ){ print("(!) Error loading %s", sign_cascade_fullpath); return false; };

    pMOG2->apply(src, fgMask, 0.3);

    vector<Rect> signs;
    Mat gray, bw;
    RNG rng(12345);

    cvtColor( src, gray, COLOR_BGR2GRAY );
    equalizeHist( gray, gray );
    
    sign_cascade.detectMultiScale( /*src*/ /*gray*/ fgMask, signs, 1.1, 3, 0|CASCADE_SCALE_IMAGE ,Size(150,150) );

    Point center(src.cols / 2, src.rows / 2);
    circle(src, center, 5, Scalar::all(0), -15, 8, 0);

    /* For testing */ 
    int minIdx = -1;
    float minDist = src.cols*src.rows;
    float maxArea = 0;
    for( int i = 0; i < signs.size(); i++ )
    {
        Rect r = signs[i];
        float count = countNonZero(Mat(fgMask, r)); 
        Point p(r.x + r.width/2,  r.y+r.height/2);
        float res = cv::norm(center-p);
        
        if (count > maxArea && res < minDist  && r.contains(center)) {
            minIdx = i;
            minDist = res;
            maxArea = count;
        }
        //Scalar color = Scalar(rng.uniform(0,255), rng.uniform(0, 255), rng.uniform(0, 255));
        //rectangle( src, signs[i].tl(), signs[i].br(),color , 3, 8, 0 );
    }
       
    bool result = false; 
    if (minIdx > -1) { 
        rectangle( src, signs[minIdx].tl(), signs[minIdx].br(), Scalar(0,0,0), 3, 8, 0 );
        result = true;
    }

    Mat left;
    //cvtColor(gray, left, COLOR_GRAY2BGR);
    cvtColor(fgMask, left, COLOR_GRAY2BGR);
    Mat img = merge(left,src); 
    show("Result", img);
    /* end testing */

    //sign_cascade.empty();
    //if (signs.size() > 0) return true;

    return result;
}

void fixRotation(Mat &src, Mat &dst, int rotation) {
    Mat original = src.clone();
    Size sz = src.size();
    rot90(src, rotation);
    //copyMakeBorder( src, dst, top, bottom, left, right, borderType, value );
    resize(src,dst,sz);
}

void rot90(Mat &src, int flag){
  //1=CW, 2=CCW, 3=180
  if (flag == 1){
    transpose(src, src);  
    flip(src, src,1); 
  } else if (flag == 2) {
    transpose(src, src);  
    flip(src, src,0); 
  } else if (flag ==3){
    flip(src, src,-1);    
  } else if (flag != 0){ 
    cout  << "Unknown rotation flag(" << flag << ")" << endl;
  }
}


/*************************DEBUG FUNCTIONS**********************************************************************/


void print(const char* format, ... ) {
#ifdef debug
    va_list args;  
    
    char buff[100]; //Buffer for the time
    
    time_t now = time (0); //Get current time object
    
    //Format time object into string using the format provided
    strftime(buff, 100, "%Y-%m-%d %H:%M:%S", localtime (&now)); 
    
    //Print time to the screen
    printf ("%s: ", buff);

    //Based on format, read in args from (...)
    va_start(args, format);
    
    //Fill-in format with args and print to screen
    vprintf(format, args);

    //Release args memory
    va_end(args);

    //Create a newline
    printf("\n");

    //Flush standard out to make sure this gets printed
    fflush(stdout);
#endif
}

void show(const char *name, Mat &img)
{
#ifdef debug
    namedWindow( name, 0 );
    imshow( name, img );
    resizeWindow(name, 800, 600);
#endif
}

Mat merge(Mat &img1, Mat &img2)
{
    Size sz1 = img1.size();
    Size sz2 = img2.size();
    //assert(sz1 == sz2);
    
    Mat img3(sz1.height, sz1.width+sz2.width, CV_8UC3);
    Mat left(img3, Rect(0, 0, sz1.width, sz1.height));
    img1.copyTo(left);
    Mat right(img3, Rect(sz1.width, 0, sz2.width, sz2.height));
    img2.copyTo(right);
    return img3;    
}

