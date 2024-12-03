from rest_framework_simplejwt.exceptions import InvalidToken, TokenError

@api_view(['POST'])
@permission_classes([AllowAny])
def login_user(request):
    try:
        # ... existing code ...

        if user:
            try:
                refresh = RefreshToken.for_user(user)
                return Response({
                    'access': str(refresh.access_token),
                    'user': {
                        'id': str(user.id),
                        'username': user.username,
                        'email': user.email,
                        'is_superuser': user.is_superuser
                    }
                })
            except (TokenError, InvalidToken) as e:
                logger.error(f"Token generation failed for user {username}: {str(e)}")
                return Response({
                    'error': 'Authentication failed. Please try again.'
                }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)
        
        # ... rest of the code ... 

@api_view(['GET'])
@permission_classes([IsAuthenticated])
def admin_all_tasks(request):
    try:
        if not request.user.is_superuser:
            return Response({
                'error': 'Admin privileges required'
            }, status=status.HTTP_403_FORBIDDEN)

        tasks = Task.objects.select_related('user').all()
        serializer = TaskSerializer(tasks, many=True)
        
        return Response({
            'tasks': serializer.data,
            'count': tasks.count()
        })

    except Exception as e:
        logger.error(f"Error in admin_all_tasks: {str(e)}")
        return Response({
            'error': 'Failed to fetch tasks'
        }, status=status.HTTP_500_INTERNAL_SERVER_ERROR)