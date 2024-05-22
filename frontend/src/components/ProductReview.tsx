import {ProductDto, ProductReviewReviewDto} from "@/lib/dto";
import React, {useEffect, useState} from "react";
import axios from "axios";
import {baseURL} from "@/lib/const";
import {errorHandler} from "@/lib/utils";
import {Avatar, Card, Divider, Rate, Space, Typography} from "antd";
import {UserOutlined} from "@ant-design/icons";
import {ProductReviewReply} from "@/components/ProductReviewReply";

const {Text, Title} = Typography;
type ProductReviewProps = {
    review: ProductReviewReviewDto;
    productState: ProductDto;
    confirmDelete: (review: ProductReviewReviewDto) => void;
    setSelectedReview: (review: ProductReviewReviewDto) => void;
    setOpenReplyModal: (open: boolean) => void;
    isMerchant: boolean;
    isAdmin: boolean;
}

export const ProductReview = ({
                                  review,
                                  productState,
                                  isMerchant,
                                  isAdmin,
                                  setSelectedReview,
                                  confirmDelete,
                                  setOpenReplyModal
                              }: ProductReviewProps) => {
    const [image, setImage] = useState<string | undefined>(undefined);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const getImage = async () => {
        setLoading(true)
        try {
            const accessToken = localStorage.getItem('accessToken');
            const response = await axios.get(`${baseURL}/users/picture/${review.userDto.id}`, {
                headers: {
                    'Authorization': `Bearer ${accessToken}`,
                    'Content-Type': 'application/json',
                },
                responseType: 'arraybuffer',
            });

            const imageBlob = new Blob([response.data], {type: 'image/png'});
            const imageUrl = URL.createObjectURL(imageBlob);
            setImage(imageUrl);
        } catch (error) {
            const errorMessage = errorHandler(error, 'Error fetching images');
            console.log(errorMessage);
        }
        setLoading(false);
    };

    useEffect(() => {
        getImage();
    }, []);

    return (
        <Card style={{marginBottom: '10px'}}
              extra={
                  <Space>
                      {isMerchant && productState.topicUserDto.username === localStorage.getItem('username') && (
                          <Typography.Link onClick={() => {
                              setSelectedReview(review);
                              setOpenReplyModal(true);
                          }}>Reply</Typography.Link>
                      )}
                      {isAdmin && (
                          <Typography.Link
                              onClick={() => confirmDelete(review)}>Delete</Typography.Link>
                      )}
                  </Space>
              }
              title={<Space><Avatar src={image} icon={<UserOutlined/>}>{review.userDto.username[0]}</Avatar><Text
                  strong>{review.userDto.username}</Text></Space>}
        >
            <Space direction="vertical" style={{width: '100%'}}>
                <Space>
                    <Rate disabled
                          allowHalf
                          defaultValue={(review.fieldScoreDtos.reduce((acc, field) => acc + field.score, 0) / review.fieldScoreDtos.length)/2}/>
                    <span>{(review.fieldScoreDtos.reduce((acc, field) => acc + field.score, 0) / review.fieldScoreDtos.length).toFixed(2)}</span>
                </Space>
                <Space direction="horizontal">
                    {review.fieldScoreDtos.map((field, index) => (
                        <div key={index}>
                            <strong>{field.reviewFieldDto.label}:</strong> {field.score}
                        </div>
                    ))}
                </Space>
                <Space>
                    {review.comment}
                </Space>
                <Divider/>
                {review.reviewReplyDto && (
                    <ProductReviewReply topicUserDto={productState.topicUserDto}
                                        reviewReplyDto={review.reviewReplyDto}/>
                )}
            </Space>
        </Card>
    )
}